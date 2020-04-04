(ns com.github.hindol.twenty-nine.engine
  (:require))

(defn play
  [hand {:keys [turns plays]}]
  (if (empty? plays)
    (rand-nth hand)
    (let [leader        (first turns)
          suit          (:suit (get plays leader))
          cards-in-suit (filter #(-> % :suit (= suit)) hand)]
      (prn hand suit cards-in-suit)
      (if (empty? cards-in-suit)
        (rand-nth hand)
        (rand-nth cards-in-suit)))))