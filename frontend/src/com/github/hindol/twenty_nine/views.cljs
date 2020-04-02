(ns com.github.hindol.twenty-nine.views
  (:require
   [clojure.pprint :as pp]
   [clojure.string :as str]
   [re-frame.core :as rf]))

(defn url-for
  [{:keys [suit rank]}]
  (str "images/cards/minified/"
       (name suit) "/"
       (let [r (str/lower-case (name rank))]
         (case r
           "jack" "j"
           "queen" "q"
           "king" "k"
           r))
       (str/lower-case (subs (name suit) 0 1)) ".svg"))

(defn card
  [c props]
  (when c
    [:img (merge {:src (url-for c)} props)]))

(defn trick
  []
  (let [{plays :plays} @(rf/subscribe [:trick])]
    [:div.columns
     [:div.column
      [:div.level
       [:div.level-item [card (:north plays)]]]
      [:div.level
       [:div.level-left [card (:west plays)]]
       [:div.level-right [card (:east plays)]]]
      [:div.level
       [:div.level-item [card (:south plays)]]]]]))

(defn show-hand
  [player]
  (let [hand @(rf/subscribe [:hand player])
        turn @(rf/subscribe [:turn])]
    (into [:div.level]
          (map (fn [c] [:div.level-item
                        [card c {:on-click #(rf/dispatch [:play c {:player player
                                                                   :turn   turn}])}]])
               hand))))

(defn app-db
  []
  [:pre (with-out-str (pp/pprint @(rf/subscribe [:app-db])))])

(defn ui
  []
  [:div.columns
   [:div.column
    [trick]
    [show-hand :south]
    [app-db]]])
