(ns com.github.hindol.twenty-nine.db)

(def suits
  [:diamonds :clubs :hearts :spades])

(def ranks
  [:7 :8 :queen :king :10 :ace :9 :jack])

(def players
  [:north :west :south :east])

(defn turns
  []
  (take 7 (cycle players)))

(def deck
  (for [s suits
        r ranks]
    {:suit s
     :rank r}))

(defn deal-half
  []
  (zipmap players (->> deck shuffle (partition 4) (map vec))))

(defn deal-remaining
  [])

(defn deal
  []
  (zipmap [:north :west :south :east] (->> deck shuffle (partition 8) (map vec))))

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
   :bidder (rand-nth players)
   :trump  {:suit    (rand-nth suits)
            :exposed false}
   :tricks {:current (trick {:leader :south})
            :past    []}})

(def app-db
  {:players {:north :machine
             :west  :machine
             :south :human
             :east  :machine}
   :rounds  {:current nil
             :past    []}})
