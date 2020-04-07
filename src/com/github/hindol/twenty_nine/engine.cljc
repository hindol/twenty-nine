(ns com.github.hindol.twenty-nine.engine
  (:require
   [com.github.hindol.twenty-nine.db :as db]
   [com.github.hindol.twenty-nine.utils :refer [position]]))

(defn candidates
  [hand {:keys [turns plays]}]
  (if (empty? plays)
    hand
    (let [leader        (first turns)
          suit          (:suit (get plays leader))
          cards-in-suit (filter #(-> % :suit (= suit)) hand)]
      (if (empty? cards-in-suit)
        hand
        cards-in-suit))))

(defn play
  [hand trick]
  (rand-nth (candidates hand trick)))

(defn winner
  [{:keys [turns plays]}]
  (let [suit          (->> turns first (get plays) :suit)
        cards-in-suit (filter #(= suit (-> % val :suit)) plays)]
    (key (apply max-key
                #(position #{(-> % val :rank)} db/ranks)
                cards-in-suit))))
