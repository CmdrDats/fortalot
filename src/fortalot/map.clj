(ns fortalot.map)

(defonce seed (int (rand (Integer/MAX_VALUE))))

(defn get-chunk
  "Given x, y and z block coordinates, returns a chunk vector [x y z], a chunk contains 8 x 8 x 2 segments"
  [[x y z]]
  [(int (Math/floor (/ x 128)))
   (int (Math/floor (/ y 128)))
   (int (Math/floor (/ z 8)))])

(defn get-segment
  "Given x, y and z block coordinates, return a segment vector [x y z]. you need to combine a segment with a chunk
   to get back to block coordinates since a segment is relative to a chunk.
   A segment represents 16 x 16 x 4 blocks"
  [[x y z]]
  [(int (mod (Math/floor (/ x 16)) 8))
   (int (mod (Math/floor (/ y 16)) 8))
   (int (mod (Math/floor (/ z 4)) 2))])

(defn get-block-in-segment
  "Given x, y and z block coords, return a position within a segment that you need to look to find the given tile.
   Returns [x z], where y is rolled into x"
  [[x y z]]
  [(int (+ (mod x 16) (* (mod y 16) 16)))
   (int (mod z 4))])

(defn get-block
  "From a chunk and segment, find where the starting corner of it is"
  [[cx cy cz] [sx sy sz]]
  [(+ (* cx 128) (* sx 16))
   (+ (* cy 128) (* sy 16))
   (+ (* cz 8) (* sz 4))])



(defn gen-chunk
  "Generate and return a chunk for a given chunk location"
  []
  {:segments {}})

(defn chunk-coords-for-view [[x y z] [w l]]
  (let [[minx miny minz] (get-chunk [x y (dec z)])
        [maxx maxy maxz] (get-chunk [(+ x w) (+ y l) (inc z)])]
    (for [x (range minx (inc maxx))
          y (range miny (inc maxy))
          z (range minz (inc maxz))]
      [x y z])))

(defn get-block-at
  [{:keys [chunks] :as world} x y z & [chunk segment]]
  (let [chunk (or chunk (get chunks (get-chunk [x y z]) {:segments {}}))
        segment (or segment (get (:segments chunk) (get-segment [x y z]) {}))
        [sx sz] (get-block-in-segment [x y z])
        plane (get segment sz nil)]
    (cond
     (nil? plane) 0
     (coll? plane) (get plane sx)
     :else plane)))

(defn fetch-view
  "This fetches a viewport of the world as seen from the x y z position with a specified width and length
   Will return a list of tiles in the format of [x y relative-z sprite-x sprite-y] in the :tiles map
   and a list of visible entities in :entities as [x y relative-z tilespec]
   - note that relative-z means 0 for current layer, 1 for above, -1 for below, -2 for further below, etc.
   - also note that the entity x, y is in block coords and can be floating point."
  [{:keys [chunks] :as world} [x y z :as pos] [w l :as size]]
  {:tiles
   (for [nx (range x (+ x w))
         ny (range y (+ y l))
         :let [chunk (get chunks (get-chunk [nx ny z]) {:segments {}})
               segment (get (:segments chunk) (get-segment [nx ny z]) {})
               block (get-block-at world nx ny z chunk segment)]
         :when (not= block 0)]
     [nx ny z block])})