(ns com.github.discoverAI.snake.engine
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as c]
            [com.github.discoverAI.snake.board :as b]
            [de.otto.status :as st]
            [de.otto.tesla.stateful.app-status :as as]
            [overtone.at-at :as at-at]
            [de.otto.tesla.stateful.scheduler :as scheduler]
            [com.github.discoverAI.snake.token :as token]))

(defn game-id [game-state]
  (->> (hash game-state)
       (+ (System/nanoTime))
       (str "G_")
       (keyword)))

(defn new-game [width height snake-length]
  (let [game-state (b/place-food (b/initial-state width height snake-length))]
    {(game-id game-state) game-state}))

(defn modulo-vector [position-vector modulos]
  (map mod position-vector modulos))

(defn vector-addition [first second]
  (map + first second))

(def MOVE_UPDATE_INTERVAL 1000)

(defn snake-on-food? [games-state]
  (= (first (get-in games-state [:tokens :snake :position]))
     (first (get-in games-state [:tokens :food :position]))))

(defn move-snake [board {:keys [direction] :as snake}]
  (update snake :position
          (fn [snake-position]
            (concat [(modulo-vector (vector-addition (first snake-position) direction) board)]
                    (drop-last snake-position)))))

(defn move [game-state]
  (update-in game-state [:tokens :snake] (partial move-snake (:board game-state))))

(defn new-direction-vector [old new]
  (if (= (vector-addition old new) [0 0])
    old
    new))

(defn change-direction [games game-id direction]
  (let [direction-path [game-id :tokens :snake :direction]
        current-dir (get-in @games direction-path)]
    (swap! games assoc-in direction-path
           (new-direction-vector current-dir direction))))

(defn update-game-state! [games-atom game-id callback-fn]
  (swap! games-atom update game-id move)
  (callback-fn (game-id @games-atom)))

(defn register-new-game [{:keys [games scheduler]} width height snake-length callback-fn]
  (let [game (new-game width height snake-length)
        game-id (first (keys game))]
    (swap! games merge game)
    (at-at/every MOVE_UPDATE_INTERVAL
                 #(update-game-state! games game-id callback-fn)
                 (scheduler/pool scheduler)
                 :desc "UpdateGameStateTask")
    game-id))

(defn games-state-status [games-state-atom]
  (if (and (map? @games-state-atom) (<= 0 (count @games-state-atom)))
    (st/status-detail :engine :ok (str (count @games-state-atom) " games registered"))
    (st/status-detail :engine :error "Engines games are corrupt")))

(defrecord Engine [app-status scheduler]
  c/Lifecycle
  (start [self]
    (log/info "-> starting Engine")
    (let [games-state (atom {})]
      (as/register-status-fun app-status (partial games-state-status games-state))
      (assoc self :games games-state)))
  (stop [_]
    (log/info "<- stopping Engine")))

(defn new-engine []
  (map->Engine {}))
