(ns com.github.hindol.twenty-nine.game
  (:require
   [com.github.hindol.twenty-nine.db :as db]
   [datascript.core :as d])
  (:import
   (java.util UUID)))

(def ^:private schema
  {:uuid       {:db/unique :db.unique/identity}
   :side       {:db/unique :db.unique/identity}
   :ws-session {:db/unique :db.unique/identity}})

(defonce ^:private ds (d/create-conn schema))

(defn uuid
  []
  (UUID/randomUUID))

(defn side
  [ws-session]
  (let [s (:side (d/entity @ds [:ws-session ws-session]))]
    s))

(defn join
  [{:keys [ws-session]}]
  (let [filled (d/q '[:find ?s
                      :where
                      [_ :side ?s]]
                    @ds)
        side   (rand-nth (remove (set filled) db/sides))
        entity {:uuid       (uuid)
                :side       side
                :ws-session ws-session}]
    (d/transact! ds [entity])
    entity))

(defn leave
  [{:keys [ws-session]}]
  (d/transact! ds [[:db.fn/retractEntity [:ws-session ws-session]]])
  (side ws-session))

(defn view-as
  [db player]
  (-> db
      (update-in [:rounds :current :hands]
                 #(-> % (select-keys [player])))
      (assoc :viewing-as player)))
