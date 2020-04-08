(ns com.github.hindol.twenty-nine.effects
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require
   [cljs-http.client :as http]
   [cljs.core.async :refer [<! >!]]
   [haslett.client :as ws]
   [haslett.format :as fmt]
   [re-frame.core :as rf]))

(def web-socket (atom nil))

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
 (fn []
   (go
     (when-let [ws-stream (<! (ws/connect (str "wss://" (.. js/document -location -hostname) "/ws")
                                          {:format fmt/edn}))]
       (reset! web-socket ws-stream)
       (loop []
         (if-let [message (<! (:source ws-stream))]
           (do
             (rf/dispatch [:on-text message])
             (recur))
           (ws/close ws-stream)))))))

(rf/reg-fx
 :send-ws
 (fn [{:keys [message]}]
   (when @web-socket
     (go
       (>! (:sink @web-socket) message)))))
