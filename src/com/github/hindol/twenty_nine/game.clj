(ns com.github.hindol.twenty-nine.game)

(defn view-as
  [db player]
  (-> db
      (update-in [:rounds :current :hands]
                 #(-> % (select-keys [player])))))
