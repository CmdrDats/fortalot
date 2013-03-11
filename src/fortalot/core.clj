(ns fortalot.core
  (:require [clojure.tools.nrepl.server :refer (start-server stop-server)]
            [fortalot.map :as map]
            [clojure.tools.logging :as log])
  (:import [org.newdawn.slick BasicGame CanvasGameContainer AppGameContainer SpriteSheet])
  (:import [org.lwjgl.opengl Display])
  (:gen-class))

(def world
  (atom
   {:tileset-file "resources/sheet2.png"
    :tileset
    {1 {:tile [0 0]}
     2 {:tile [0 1]}
     3 {:tile [0 2]}}
    :chunks
    {[0 0 0]
     {:segments
      {[0 0 0]
       [0 0 0 1]
       [1 0 0]
       [0 0 0
        [1 1 1 3 0 0 0 0 0 0 0 0 0 0 0 0
         1 1 2 3 0 0 0 0 0 0 0 0 0 0 0 0
         1 1 2 3 0 0 0 0 0 0 0 0 0 0 0 0
         1 1 2 3 3 3 3 0 0 0 0 0 0 0 0 0
         1 1 2 2 1 1 1 3 0 0 0 0 0 0 0 0
         1 1 2 2 2 2 2 2 3 0 0 0 0 0 0 0
         0 1 1 1 1 1 1 2 3 0 0 0 0 0 0 0
         0 0 0 0 1 3 1 2 3 0 0 0 0 0 0 0
         0 0 0 0 1 3 1 2 3 0 0 0 0 0 0 0
         0 0 0 0 1 1 1 2 3 0 0 0 0 0 0 0
         0 0 0 0 0 0 1 1 3 0 0 0 0 0 0 0
         0 0 0 0 0 0 0 0 3 0 0 0 0 0 0 0
         0 0 0 0 0 0 0 0 3 0 0 0 0 0 0 0
         0 0 0 0 0 0 0 0 0 3 0 0 0 0 0 0
         0 0 0 0 0 0 0 0 0 0 3 3 0 0 0 0
         0 0 0 0 0 0 0 0 0 0 0 0 3 0 0 0]]
       [0 1 0] [0 0 0 1]
       [1 1 0] [0 0 0 1]}}}}))


;; A viewport position points to the center of the view, and as x y z
;; and size in blocks
(defonce viewport (atom {:location [0 0 0] :size [0 0]}))

(defonce context (atom nil))

(defonce paused (atom false))

(def spritesheet (atom nil))

(defn reload-sheet []
                                        ;(.reload (org.newdawn.slick.opengl.InternalTextureLoader/get))
  (.clear (org.newdawn.slick.opengl.InternalTextureLoader/get))
  (reset! spritesheet (SpriteSheet. (:tileset-file @world) 16 16)))

(def offset (atom 0.0))

(defn pause [pause?]
  (doto @context
    (.setPaused pause?)
    (.setUpdateOnlyWhenVisible pause?)
    (.setAlwaysRender (not pause?))
    (.setTargetFrameRate (if pause? -1 60)))
  (reset! paused pause?))

(defn render-game [gc g]
  (try
    ;(reload-sheet)
    (let [sp @spritesheet
          in (.getInput gc)
          [width height] (:size @viewport)
          tileset (:tileset @world)]
      (.startUse sp)
      (try
        (doseq [[x y z b] (:tiles (map/fetch-view @world [0 0 3] [width height]))
                :let [[tx ty] (:tile (get tileset b))]]
          (.renderInUse sp (* (- x 10) 32) (* (- y 2) 32) 32 32 tx ty))
        (.renderInUse sp (* (int (/ (.getMouseX in) 32)) 32) (* (int (/ (.getMouseY in) 32)) 32) 32 32 0 3)
        (finally (.endUse sp)))
      (.drawString g (str (.getMouseX in) " x " (.getMouseY in) " - " @offset) 100 100))
    (catch Exception e
      (.printStackTrace e)
      (pause true))))

(defn update-viewport-size! [gc]
  (swap! viewport update-in [:size] (fn [_] [(inc (/ (.getWidth gc) 16)) (inc (/ (.getHeight gc) 16))])))

#_(def mouse-click-point
  (atom ))

(defn update-game 
  "Updating game"
  [gc delta]
  (when (Display/wasResized)
    (log/info "Resized game")
    (update-viewport-size! gc))
  (if @paused (Thread/sleep 1000))
  (try
    (swap! offset #(+ % (/ delta 100)))
    (.setTargetFrameRate gc 200)
    (catch Exception e
      (.printStackTrace e)
      (pause true)))) 

(defn init-game [gc]
  (.setHoldTextureData (org.newdawn.slick.opengl.InternalTextureLoader/get) true)
  (reset! spritesheet (SpriteSheet. (:tileset-file @world) 16 16))
  (reset! context gc))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (start-server :host "0.0.0.0" :port 4005)
  (let [hellogame
        (proxy [BasicGame] ["Fortalot"]
          (init [gc] 
            (init-game gc))
          (update [gc delta]
            (update-game gc delta))
          (render [gc g]
            (render-game gc g)))
        gc (AppGameContainer. hellogame)]
    (doto gc
      (.setTargetFrameRate 60)
      (.setUpdateOnlyWhenVisible false)
      (.setResizable true)
      (.setAlwaysRender true)
      (.setDisplayMode 800 600 false))
    (update-viewport-size! gc)
    (comment
      (let [frame (javax.swing.JFrame. "Fortalot")]
        (.add frame gc)
        (.setSize frame 800 600)
        (.show frame)))
    (.start gc)
    ))