(ns flux-challenge-re-frame.handlers
  (:require
    [clojure.walk :refer [keywordize-keys]]
    [cljs.core.async :refer [<!]]
    [re-frame.core :as re-frame :refer [dispatch register-handler]]
    [cljs-http.client :as http]
    [chord.client :refer [ws-ch]]
    )
  (:require-macros
    [cljs.core.async.macros :refer [go]]
    )
  )

(def ^{:private true} jedi-url "http://localhost:3000/dark-jedis/")
(def ^{:private true} planets-url "ws://localhost:4000")

(defn- get-jedi
  "Fetches info about Jedi. Returns a channel with:
    * a map when the request was successful
    * a channel with nil when the request was unsuccessful or nil was given as an argument"
  [jedi-id]
  (go
    (when jedi-id
      (let [response (<! (http/get (str jedi-url jedi-id) {:with-credentials? false}))]
        (if (:success response)
          (:body response)
          ; console.log for that sweet object logging
          (.log js/console (str "Problems fetching Jedi " jedi-id ". Response:") (clj->js response)))))))


(defn- change-current-planet [db, [_ planet]]
  (assoc db :planet planet))

(register-handler :change-current-planet
                  change-current-planet)

(defn- monitor-planets [db _]
  (do
    ; Open a new websocket connection only if there's no planet in state.
    ; Without this we'd create a new websocket connection on each figwheel reload.
    (when-not (get-in db [:planet :id])
      (go
        (let [{:keys [ws-channel error]} (<! (ws-ch planets-url {:format :json}))]
          (if-not error
            (loop []
              ; If the websocket server gets closed, the ws-channel gets closed too and the response will be nil.
              (let [response (<! ws-channel)
                    error (:error response)
                    planet (keywordize-keys (:message response))]
                (if planet
                  (do
                    (dispatch [:change-current-planet planet])
                    (recur))
                  (if error
                    (println (str "Error while receiving message: " error))
                    (println "Websocket connection closed unexpectedly")))))
            (println (str "Error while connecting: " error))))))
    db))

(register-handler :monitor-planets
                  monitor-planets)

(defn- save-jedi
  [db [_ jedi]]
  (assoc db :jedis (conj (:jedis db) jedi)))

(register-handler :save-jedi
                  save-jedi)

(defn- populate-jedis [db _]
  (do
    (when-not (seq (:jedis db))
      ; Weird, when using go-loop, I'm getting exactly the same error as presented here:
      ; https://theholyjava.wordpress.com/2014/05/12/core-async-cant-recur-here-in-clojurescript-but-ok-in-clojure/
      (go
        (loop [jedi-id (:first-jedi-id db)]
          (when-let [jedi (<! (get-jedi jedi-id))]
            (dispatch [:save-jedi jedi])
            (recur (get-in jedi [:apprentice :id]))))))
    db))

(register-handler :populate-jedis
                  populate-jedis)
