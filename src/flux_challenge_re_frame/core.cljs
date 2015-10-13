(ns ^:figwheel-always flux-challenge-re-frame.core
  (:require
    [clojure.walk :refer [keywordize-keys]]
    [reagent.core :as reagent]
    [chord.client :refer [ws-ch]]
    [cljs.core.async :refer [<!]]
    [cljs-http.client :as http]
    [re-frame.core :as re-frame :refer [subscribe register-sub
                                        dispatch register-handler]])
  (:require-macros
    [cljs.core.async.macros :refer [go]]
    [reagent.ratom :refer [reaction]]))

(enable-console-print!)

; I don't use prismatic/schema, because I don't want to make the code harder to read for
; someone who doesn't know much ClojureScript.
; On the other hand, using records seems to be useless in this case, see:
; https://twitter.com/Ravicious/status/653604805886275584

; However, I still want to have a place which tells me which fields a given map has . ;)
; (defrecord Planet [id name])
; (defrecord Jedi [id name homeworld apprentice master])

(def jedi-url "http://localhost:3000/dark-jedis/")
(def planets-url "ws://localhost:4000")

(defonce initial-state {:planet {:id nil :name "unknown"}
                        :jedis []
                        :first-jedi-id 3616
                        :db-initialized? true})

(defn pad
  "Pads the collection coll to the given length n with val"
  [n val coll]
  (take n (concat coll (repeat val))))

(defn get-jedi
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

; At the start of the application, the re-frame's db is empty. We need to populate it by dispatching
; the initialize-db event, effectively putting contents of the initial-state map into re-frame's db.
(register-handler
  :initialize-db
  (fn [db _]
    (if (:db-initialized? db)
      db
      initial-state)))

;;; subs

(defn jedis [db, _]
  (reaction (:jedis @db)))

(register-sub :jedis jedis)

(defn padded-jedis [db, [_, count]]
  (->> @(jedis db _)
       (pad count nil)
       reaction))

(register-sub :padded-jedis padded-jedis)

(defn current-planet [db, _]
  (reaction (:planet @db)))

(register-sub :current-planet current-planet)

;;; handlers

(defn change-current-planet [db, [_ planet]]
  (assoc db :planet planet))

(register-handler :change-current-planet
                  change-current-planet)

(defn monitor-planets [db _]
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

(defn save-jedi
  [db [_ jedi]]
  (assoc db :jedis (conj (:jedis db) jedi)))

(register-handler :save-jedi
                  save-jedi)

(defn populate-jedis [db _]
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

;;; initial dispatches

(dispatch [:initialize-db])
(dispatch [:monitor-planets])
(dispatch [:populate-jedis])

;;; components

(defn planet-monitor [planet]
  [:h1 {:class "css-planet-monitor"}
   (str "Obi-Wan currently on " (:name planet))])

(defn jedi-slot [jedi]
  [:li {:class "css-slot"}
   (when jedi
     [:span
      [:h3 (:name jedi)]
      [:h6 (str "Homeworld: " (get-in jedi [:homeworld :name]))]])])

(defn dark-jedi-list []
  (let [current-planet (subscribe [:current-planet])
        padded-jedis (subscribe [:padded-jedis 5])]
    (fn []
      [:div {:class "css-root"}
       [planet-monitor @current-planet]

       [:section {:class "css-scrollable-list"}
        [:ul {:class "css-slots"}
         (for [jedi @padded-jedis]
           (let [key (or
                       (:name jedi)
                       (gensym "jedi"))] ; use random name if no name present
             ^{:key key} [jedi-slot jedi]))]

        [:div {:class "css-scroll-buttons"}
         [:button {:class "css-button-up"}]
         [:button {:class "css-button-down"}]]]])))

(reagent/render-component
  [dark-jedi-list]
  (. js/document (getElementById "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )
