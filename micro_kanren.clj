(clojure/in-ns 'kanren)
(clojure/refer 'clojure)

; Point 1: Functions can have more (or less) than one result
;
; We represent a 'relation' (aka 'non-deterministic function') as a regular
; clojure function that returns a list of possible results.

; First, we define two primitive non-deterministic functions; one yields
; no result for any argument; the other merely returns its argument as the 
; sole result.
(defn f [x] [])
(defn t [x] [x])

;
(defn || [f1 f2] 
  (fn [x] (lazy-cat (f1 x) (f2 x))))
(defn && [f1 f2] 
  (fn [x] (apply concat (map f2 (f1 x)))))

(print
  ((||
    (|| f t)
    (&&
      (|| (fn [x] (t (+ x 1)))
          (fn [x] (t (+ x 10))))
      (|| t t)))
   100))