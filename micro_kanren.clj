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

; Logical disjunction
(defn || [f1 f2] 
  (fn [x] (lazy-cat (f1 x) (f2 x))))

; Logical conjunction
(defn && [f1 f2] 
  (fn [x] (apply concat (map f2 (f1 x)))))

; Tests
(println ((|| f t) 100))
(println ((&& t t) 100))
(println ((|| t t) 100))

(println
  ((||
    (|| f t)
    (&&
      (|| (fn [x] (t (+ x 1)))
          (fn [x] (t (+ x 10))))
      (|| t t)))
   100))

; Point 2: (Prolog-like) Logic variables

; We represent logic variables with Clojure struct-maps
(defstruct logical-var :name)
(defn lvar [name] (struct logical-var name))
(defn lvar? [v] (and (= clojure.lang.PersistentStructMap (class v)) (:name v)))

; We implement associations of logic variables and their values 
; (aka substitutions) with Clojure maps.
(defn empty-subst [] {})
(defn ext-s [var value substs] (assoc substs var value))

; Find the value associated with var in substitution s.
; Return var itself if it is unbound.
; In miniKanren, this function is called 'walk'
(defn lookup [var s]
  (cond
    (not (lvar? var)) var
    (contains? s var) (lookup (get s var) s)
    true var)) ;; no else??

(defn pair? [v] (and (vector? v) (= 2 (count v))))

(defn unify [t1 t2 s]
  (let [t1 (lookup t1 s)
        t2 (lookup t2 s)]
    (cond
      (= t1 t2) s                  ; t1 and t2 the same, no new knowledge
      (lvar? t1) (ext-s t1 t2 s)   ; t1 unbound
      (lvar? t2) (ext-s t2 t1 s)   ; t2 unbound
      (and (pair? t1) (pair? t2))  ; if t1 is a pair so is t2
        (let [s (unify (first t1) (first t2) s)]
          (and s (unify (frest t1) (frest t2) s)))
      (identical? t1 t2) s       ; t1 and t2 are the same value
      true false)))

(def vx (lvar 'x))
(def vy (lvar 'y))
(def vz (lvar 'z))
(def vq (lvar 'q))

(println (unify vx vy (empty-subst)))
(println (unify vx 1 (unify vx vy (empty-subst))))
(println (lookup vy (unify vx 1 (unify vx vy (empty-subst)))))
(println (unify [vx vy] [vy 1] (empty-subst)))


; Part 3: Logic system
;
; Now we can combine non-deterministic functions (part 1) and
; the representation of knowledge in substitutions (part 2) into a 
; logic system.
