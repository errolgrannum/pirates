(ns pirates.actions
  (:require
    [pirates.pieces :as pieces]))

(defn draw
  "Draw n cards. Note that rather than having a deck we assume an
  infinite number of uniformly distributed cards."
  [n] (repeatedly n #(rand-nth (vec pieces/card-types))))

(defn cards-to
  ([player card](update-in player [card] #(inc %)))
  ([player card & more](reduce #(cards-to %1 %2) (cards-to player card) more)))

(defn piece-count
  ([piece-slot] (reduce + (vals piece-slot)))
  ([piece-slots index] (piece-count (get piece-slots index))))

(defn space-available?
  ([piece-slots index] (and (< index (count piece-slots)) (space-available? (get piece-slots index))))
  ([piece-slot](< 0 (or (piece-count piece-slot) 0) 3)))

(defn space-occupied? [game-state index]
  (not= (piece-count (get-in game-state [:board :pieces index])) 0))

(defn pirate-on-space? [pirate game-state index]
  (if (get-in game-state [:board :pieces index pirate]) true false))

(defn find-pirates [pirate game-state]
  (let [n (count (get-in game-state [:board :pieces]))]
    (filter #(> (or (get-in game-state [:board :pieces % pirate]) 0) 0) (range n))))

(defn symbol-indices
  ([symbol board start] (filter #(= symbol (get board %)) (range start (count board))))
  ([symbol board] (symbol-indices symbol board 0)))

(defn next-open [start-index symbol board]
  (or
    (first
      (filter #(not (space-occupied? (:pieces board) %)) (symbol-indices symbol (:symbols board) start-index)))
    (dec (count board))))

(defn next-fallback [start-index piece-slots]
  (first (filter #(space-available? piece-slots %) (range (dec start-index) 0 -1))))

(defn has-card? [card color game-state](< 0 (or (get-in game-state [:players color :cards card]) 0)))

(defn has-color? [color index game-state](< 0 (or (get-in game-state [:board :pieces index color]) 0)))

(defn execute-play-card [card-symbol pirate-color from-index game-state]
  (let [to-index (next-open (inc from-index) card-symbol (get-in game-state [:board]))
        removed (update-in game-state [:board :pieces from-index pirate-color] dec)
        added (update-in removed [:board :pieces to-index pirate-color] inc)
        de-carded (update-in added [:players pirate-color :cards card-symbol] dec)]
    de-carded))

(defn play-card [card-symbol pirate-color from-index game-state]
  (if (and (has-card? card-symbol pirate-color game-state)
           (has-color? pirate-color from-index game-state))
    (execute-play-card card-symbol pirate-color from-index game-state)
    game-state))

(defn execute-fall-back [from-index to-index pirate game-state]
  (let [removed (update-in game-state [:board :pieces from-index pirate] dec)
        num-cards (piece-count (get-in game-state [:board :pieces to-index]))
        added (update-in removed [:board :pieces to-index pirate] inc)]
    (update-in added [:players pirate :cards] #(apply cards-to % (draw num-cards)))))

(defn fall-back [pirate start-index game-state]
  (let [to-index (next-fallback start-index (get-in game-state [:board :pieces]))]
    (if (and to-index (has-color? pirate start-index game-state))
      (execute-fall-back start-index to-index pirate game-state)
      game-state)))

(defn winner? [game-state color] (= 6 (color (last (get-in game-state [:board :pieces])))))

(defn cards [game-state color] (get-in game-state [:players color :cards]))

(defn available-cards [game-state color] (filter #(> (val %) 0) (cards game-state color)))