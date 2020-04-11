(ns com.github.hindol.twenty-nine.db
  (:require
   [com.github.hindol.twenty-nine.utils :refer [position]]))

(def suits
  [:diamonds :clubs :hearts :spades])

(def ranks
  [:7 :8 :queen :king :10 :ace :9 :jack])

(def sides
  [:north :west :south :east])

(defn turns
  []
  (take 7 (cycle sides)))

(def deck
  (for [s suits
        r ranks]
    {:suit s
     :rank r}))

(defn sort-hand
  [cards]
  (vec
   (sort-by (juxt #(position #{(:suit %)} suits)
                  #(position #{(:rank %)} ranks))
            cards)))

(defn deal-half
  []
  (zipmap sides (->> deck shuffle (partition 4) (map vec))))

(defn deal-remaining
  [])

(defn deal
  []
  (zipmap [:north :west :south :east] (->> deck shuffle (partition 8) (map sort-hand))))

(defn trick
  [{:keys [leader]}]
  {:turns (->> (turns)
               (drop-while (complement #{leader}))
               (take 4)
               vec)
   :plays {}})

(defn round
  []
  {:hands  (deal)
   :bidder (rand-nth sides)
   :trump  {:suit    (rand-nth suits)
            :exposed false}
   :tricks {:current (trick {:leader :south})
            :past    []}})

(defn game
  []
  {:current (round)
   :past    []})

;; In the backend, this is the main game state.
;; In the frontend, this is the last synced version from the server.
(def app-db
  (atom {:players {:north :machine
                   :west  :machine
                   :south :machine
                   :east  :machine}}))
