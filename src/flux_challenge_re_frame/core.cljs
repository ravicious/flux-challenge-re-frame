(ns ^:figwheel-always flux-challenge-re-frame.core
  (:require
    [clojure.walk :refer [keywordize-keys]]
    [reagent.core :as reagent :refer [atom]]
    [chord.client :refer [ws-ch]]
    [cljs.core.async :refer [<!]]
    [cljs-http.client :as http])
  (:require-macros
    [cljs.core.async.macros :refer [go]]))

(enable-console-print!)

; I don't use prismatic/schema, because I don't want to make the code harder to read for
; someone who doesn't know much ClojureScript.
; On the other hand, using records seems to be useless in this case, see:
; https://twitter.com/Ravicious/status/653604805886275584

; However, I still want to have a place which tells me which fields a given map has . ;)
; (defrecord Planet [id name])
; (defrecord Jedi [id name homeworld apprentice master])

(def jedi-url "http://localhost:3000/dark-jedis/")

(defonce app-state (atom {:planet {:id nil :name "unknown"}
                          :jedis []
                          :first-jedi-id 3616}))

(defn pad
  "Pads the collection coll to the given length n with val"
  [n val coll]
  (take n (concat coll (repeat val))))

; Open a new websocket connection only if there's no planet in state.
; Without this we'd create a new websocket connection on each figwheel reload.
(when-not (get-in @app-state [:planet :id])
  (go
    (let [{:keys [ws-channel error]} (<! (ws-ch "ws://localhost:4000" {:format :json}))]
      (if-not error
        (while true
          (let [response (<! ws-channel)
                error (:error response)
                planet (keywordize-keys (:message response))]
            (if-not error
              (swap! app-state assoc :planet planet)
              (println (str "Error while receiving message: " error)))))
        (println (str "Error while connecting: " error))))))

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

(defn save-jedi [jedi]
  (swap! app-state assoc :jedis (conj (:jedis @app-state) jedi)))

(when-not (seq (:jedis @app-state))
  ; Weird, when using go-loop, I'm getting exactly the same error as presented here:
  ; https://theholyjava.wordpress.com/2014/05/12/core-async-cant-recur-here-in-clojurescript-but-ok-in-clojure/
  (go
    (loop [jedi-id (:first-jedi-id @app-state)]
      (when-let [jedi (<! (get-jedi jedi-id))]
        (save-jedi jedi)
        (recur (get-in jedi [:apprentice :id]))))))

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
  (let [jedis (pad 5 nil (:jedis @app-state))]
    [:div {:class "css-root"}
     [planet-monitor (:planet @app-state)]

     [:section {:class "css-scrollable-list"}
      [:ul {:class "css-slots"}
       (for [jedi jedis]
         (let [key (or
                     (:name jedi)
                     (gensym "jedi"))] ; use random name if no name present
         ^{:key key} [jedi-slot jedi]))]

      [:div {:class "css-scroll-buttons"}
       [:button {:class "css-button-up"}]
       [:button {:class "css-button-down"}]]]]))

(reagent/render-component
  [dark-jedi-list]
  (. js/document (getElementById "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )
