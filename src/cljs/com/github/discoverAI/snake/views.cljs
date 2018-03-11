(ns com.github.discoverAI.snake.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [com.github.discoverAI.snake.subs :as subs]))

(def table [:table.container])

(defn pos-is-snake [snake pos]
  (boolean ((into #{} snake) pos)))

(defn cell-item-for [current-pos snake]
  (cond
    (pos-is-snake snake current-pos)
    [:td.snake]
    :else
    [:td.cell]))

(defn create-row [width y snake]
  (into [:tr {:key y}]
        (for [x (range width)
              :let [current-pos [x y]]]
          (cell-item-for current-pos snake))))

(defn render-board [width height snake]
  (let [cells (for [y (range height)]
                (create-row width y snake))]
    (conj table [:tbody cells])))

(defn board []
  (let [[grid-width grid-height] @(subscribe [:game-board-bounds])
        snake-positions @(subscribe [:snake-position])]
    (render-board grid-width grid-height snake-positions)))

(defn main-panel []
  [board])