(ns com.github.discoverAI.snake.engine-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.util.test-utils :as tu]
            [com.github.discoverAI.snake.core :as co]
            [com.github.discoverAI.snake.engine :as eg]
            [clojure.string :as s]
            [com.github.discoverAI.snake.board-test :as bt]
            [com.github.discoverAI.snake.board :as b]
            [com.github.discoverAI.snake.token :as t]))

(def game-20-20-3
  {:board  [20 20]
   :tokens {:snake {:position  [[11 10] [10 10] [9 10]]
                    :direction [1 0]
                    :speed     1.0}
            :food  {:position [[1 2]]}}})

(def game-20-20-3-id :foobar)

(deftest game-id-test
  (testing "Should calculate a unique game id"
    (is (s/starts-with? (name (eg/game-id game-20-20-3)) "G_"))

    (is (< 2
           (count (name (eg/game-id game-20-20-3)))))

    (is (not= (name (eg/game-id {:board  [21 21]
                                 :tokens {:snake {:position  [[11 10] [10 10] [9 10]]
                                                  :direction [1 0]
                                                  :speed     1.0}}}))
              (name (eg/game-id game-20-20-3))))

    (is (not= (name (eg/game-id game-20-20-3))
              (name (eg/game-id game-20-20-3))))))

(deftest new-game-test
  (testing "Should create a new game with game id"
    (with-redefs [eg/game-id (constantly game-20-20-3-id)
                  t/random-food-position (constantly [[1 2]])]
      (is (= {game-20-20-3-id game-20-20-3}
             (eg/new-game 20 20 3)))

      (is (not= {game-20-20-3-id game-20-20-3}
                (eg/new-game 21 19 7))))))

(deftest register-new-game-test
  (testing "Should add a new game to component and register move function with scheduler"
    (let [moved? (atom false)]
      (with-redefs [t/random-food-position (constantly [[1 2]])
                    eg/game-id (fn [game-state]
                                 (is (= game-20-20-3 game-state))
                                 game-20-20-3-id)
                    eg/move (fn [game-state]
                              (is (= game-20-20-3 game-state))
                              (reset! moved? true)
                              game-state)]
        (tu/with-started [system (co/snake-system {})]
                         (is (not= nil
                                   (:engine system)))

                         (is (= game-20-20-3-id
                                (eg/register-new-game (:engine system) 20 20 3 (constantly nil))))

                         (is (= game-20-20-3
                                (game-20-20-3-id @(get-in system [:engine :games]))))

                         (tu/eventually (is (= true @moved?))))))))

(deftest test-vector-add
  (testing "On a tick, the snake should move one pixel into the given direction"
    (is (= [1 4]
           (eg/vector-addition [1 2] [0 2])))))

(deftest test-new-direction
  (testing "On attempt to turn the snake 180° around or in the same direction the direction vector should stay the same"
    (is (= [1 0] (eg/new-direction-vector [1 0] [-1 0])))
    (is (= [1 0] (eg/new-direction-vector [1 0] [1 0])))))

(deftest test-new-direction
  (testing "On a valid direction update, the direction is updated"
    (is (= [0 1] (eg/new-direction-vector [1 0] [0 1])))
    (is (= [0 -1] (eg/new-direction-vector [1 0] [0 -1])))))

(deftest test-vector-modulo
  (testing "On board overflow, apply modulo operation to each elements of first vector with second one."
    (is (= [0 0]
           (eg/modulo-vector [4 0] [4 4])))

    (is (= [3 0]
           (eg/modulo-vector [3 4] [4 4])))))

(deftest move-the-snake
  (testing "move snake one pixel into the given direction"
    (is (= {:board  [20 20]
            :tokens {:snake {:position  [[12 10] [11 10] [10 10]]
                             :direction [1 0]
                             :speed     1.0}
                     :food  {:position [[1 2]]}}}
           (eg/move game-20-20-3))))

  (testing "move snake back to the left side of the field, when it overflows the field on the right side"
    (is (= {:board  [4 4]
            :tokens {:snake {:position  [[0 0] [3 0] [2 0]]
                             :direction [1 0]
                             :speed     1.0}}}
           (eg/move {:board  [4 4]
                     :tokens {:snake {:position  [[3 0] [2 0] [1 0]]
                                      :direction [1 0]
                                      :speed     1.0}}})))))

(deftest snake-on-food?-test
  (testing "should return true if snake head coincident with food"
    (is (eg/snake-on-food? {:board  [4 4]
                            :tokens {:snake {:position [[0 0] [1 0] [2 0]]}
                                     :food  {:position [[0 0]]}}})))
  (testing "should return false if snake is not on a food token"
    (is (not (eg/snake-on-food? {:board  [4 4]
                                 :tokens {:snake {:position [[1 0] [2 0] [3 0]]}
                                          :food  {:position [[0 0]]}}})))))

(deftest change-direction-test
  (testing "should change direction")
  (let [games (atom {:G_2015683382577 {:tokens {:snake {:direction [1 0]}}}})]
    (eg/change-direction games :G_2015683382577 [0 -1])
    (is (= [0 -1]
           (get-in @games [:G_2015683382577 :tokens :snake :direction])
           ))))