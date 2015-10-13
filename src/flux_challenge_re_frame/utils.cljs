(ns flux-challenge-re-frame.utils)

(defn pad
  "Pads the collection coll to the given length n with val"
  [n val coll]
  (take n (concat coll (repeat val))))
