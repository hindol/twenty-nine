(ns com.github.hindol.twenty-nine.db)

(defn turns
  []
  (cycle [:north :west :south :east]))

(def suits
  [:diamonds :clubs :hearts :spades])

(def ranks
  [:1 :7 :8 :9 :10 :jack :queen :king])

(def deck
  (for [s suits
        r ranks]
    {:suit s
     :rank r}))

(defn deal-half
  [deck]
  (zipmap [:north :west :south :east] (->> deck shuffle (partition 4))))

(defn deal-remaining
  [])

(defn trick
  [{:keys [leader]}]
  {:turns (->> (turns)
               (drop-while (complement #{leader}))
               (take 4)
               vec)
   :plays {}})

(defn round
  [{:keys [hands]}]
  {:hands  hands
   :trump  {:suit    :clubs
            :exposed false}
   :tricks {:current (trick {:leader :south})
            :past    []}})

(def app-db
  {:rounds {:current (round {:hands (deal-half deck)})
            :past    []}})

(defn play
  [])