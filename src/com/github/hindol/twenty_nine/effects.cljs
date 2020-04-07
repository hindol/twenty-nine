(ns com.github.hindol.twenty-nine.effects
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require
   [cljs-http.client :as http]
   [cljs.core.async :refer [<!]]
   [haslett.client :as ws]
   [haslett.format :as fmt]
   [re-frame.core :as rf]))

(rf/reg-fx
 :http
 (fn [{:keys [url method opts on-success on-failure]}]
   (go
     (let [http-fn                (case method
                                    :post http/post :get http/get
                                    :put http/put :delete http/delete)
           response               (<! (http-fn url opts))
           {:keys [success body]} response]
       (if success
         (rf/dispatch (conj on-success body))
         (rf/dispatch (conj on-failure body)))))))

(rf/reg-fx
 :connect-ws
 (fn [{:keys [on-text]}]
   (go
     (let [ws-stream (<! (ws/connect (str "ws://" (.. js/document -location -hostname) ":8080/ws")
                                     {:format fmt/edn}))]
       (loop []
         (if-let [edn (<! (:source ws-stream))]
           (do
             (rf/dispatch (conj on-text edn))
             (recur))
           (ws/close ws-stream)))))))
