(ns sicp-in-clojure.chap2.ex-2-73-pre
  (:refer-clojure :exclude [get]))

(defmacro ? [x] `(let [x# ~x] (println "dbg:" '~x "=" x#) x#))

;; Basic math

(defn gcd[a b]
  (if (= b 0)
    a
    (recur b (mod a b))))

(defn sqrt[x]
  (java.lang.Math/sqrt x))

(defn square [x]
  (java.lang.Math/pow x  2))

(defn atan [x y]
  (java.lang.Math/atan2 x y))

(defn sin [x]
  (java.lang.Math/sin x))

(defn cos [x]
  (java.lang.Math/cos x))

(defn pair? [c]
  (and (seq? c) (> (count c) 1)))

;; Type operations

(defn attach-tag [type-tag contents]
  (cons type-tag contents))

(defn type-tag [datum]
  ( if (pair? (? datum))
    (first datum)
    (throw (Exception. (str "Bad tagged datum -- TYPE-TAG " datum)))))

(defn contents [datum]
  (if (pair? datum)
    (rest datum)
    (throw (Exception. (str "Bad tagged datum -- CONTENTS " datum)))))

;; Dynamic dispatch map

(def op-map (atom {}))

(defn put [op type item]
  (swap! op-map assoc (list op type) item))

(defn get [op type]
  (@op-map (list op type)))


;; The magical dynamic dispatch

(defn apply-generic [op & args]
  (let [type-tags (map type-tag (? args))]
    (let [proc (get (? op) (? type-tags))]
      (if proc
        (apply proc (map (? contents) args))
        (throw (Exception.
                (str "No method for these types -- APPLY-GENERIC " op type-tags)))))))


;; Packages

;; Note below that make-from-real-image and make-from-mag-ang have to be "re-tagged" so that there is
;; no ambiguity about the coordinate system of the values.

(defn install-rectangular-package []
  (let [real-part (fn [z] (first z))
        imag-part (fn [z] (first (rest z)))
        make-from-real-imag (fn [x y] (cons x (list y)))
        make-from-mag-ang (fn [r a] (cons (* r (cos a)) (list (* r (sin a)))))
        magnitude (fn [z] (sqrt (+ (square (real-part z))
                                  (square (imag-part z)))))
        angle (fn [z] (atan (imag-part z)
                           (real-part z)))
        tag (fn [x] (attach-tag 'rectangular x))]
    (put 'real-part '(rectangular) real-part)
    (put 'imag-part '(rectangular) imag-part)
    (put 'magnitude '(rectangular) magnitude)
    (put 'angle '(rectangular) angle)
    (put 'make-from-real-imag 'rectangular
         (fn [x y] (tag (make-from-real-imag x y))))
    (put 'make-from-mag-ang 'rectangular
         (fn [r a] (tag (make-from-mag-ang r a))))))

(install-rectangular-package)

(defn real-part [z] (apply-generic 'real-part z))

(defn imag-part [z] (apply-generic 'imag-part z))

(defn magnitude [z] (apply-generic 'magnitude z))

(defn angle [z] (apply-generic 'angle z))

(defn make-from-real-imag [x y]
  ((get 'make-from-real-imag 'rectangular) x y))


;;;; Polar

(defn install-polar-package []
  (let [magnitude (fn [z] (first z))
        angle (fn [z] (first (rest z)))
        make-from-mag-ang (fn [r a] (cons r (list a)))
        real-part (fn [z] (* (magnitude z) (cos (angle z))))
        imag-part (fn [z] (* (magnitude z) (sin (angle z))))
        make-from-real-imag (fn [x y]
                                    (cons (sqrt (+ (square x) (square y))) (list (atan y x))))
        tag (fn [x] (attach-tag 'polar x))]
    (put 'real-part '(polar) real-part)
    (put 'imag-part '(polar) imag-part)
    (put 'magnitude '(polar) magnitude)
    (put 'angle '(polar) angle)
    (put 'make-from-real-imag 'polar
         (fn [x y] (tag (make-from-real-imag x y))))
    (put 'make-from-mag-ang 'polar
         (fn [r a] (tag (make-from-mag-ang r a))))))

(install-polar-package)

(defn make-from-mag-ang [r a]
  ((get 'make-from-real-imag 'polar) r a))


;; Scheme number package

(defn install-scheme-number-package []
  (let [tag (fn [x]
              (attach-tag 'scheme-number x))]
    (put 'add '(scheme-number scheme-number)
         (fn [x y] (tag (+ x y))))
    (put 'sub '(scheme-number scheme-number)
         (fn [x y] (tag (- x y))))
    (put 'mul '(scheme-number scheme-number)
         (fn [x y] (tag (* x y))))
    (put 'div '(scheme-number scheme-number)
         (fn [x y] (tag (/ x y))))
    (put 'make 'scheme-number
         (fn [x] (tag (list x))))))

(install-scheme-number-package)

(defn add [x y] (apply-generic 'add x y))

(defn sub [x y] (apply-generic 'sub x y))

(defn mul [x y] (apply-generic 'mul x y))

(defn div [x y] (apply-generic 'div x y))

(defn make-scheme-number [n]
  ((get 'make 'scheme-number) n))


;; Rational numbeer package

(defn install-rational-package []
  (let [numer (fn [x]  (first x))
        denom (fn [x] (fnext x))
        make-rat (fn [n d]
                   (let [g (gcd n d)] (cons (/ n g) (list (/ d g)))))
        add-rat (fn [x y] (make-rat (+ (* (numer x) (denom y))
                                      (* (numer y) (denom x))) (* (denom x) (denom y))))
        sub-rat (fn [x y] (make-rat (- (* (numer x) (denom y))
                                      (* (numer y) (denom x))) (* (denom x) (denom y))))
        mul-rat (fn [x y] (make-rat (* (numer x) (numer y))
                                   (* (denom x) (denom y))))
        div-rat (fn [x y]
                  (make-rat (* (numer x) (denom y)) (* (denom x) (numer y))))
        tag (fn [x] (attach-tag 'rational x))]
    (put 'add '(rational rational)
         (fn [ x y] (tag (add-rat x y))))
    (put 'sub '(rational rational)
         (fn [x y] (tag (sub-rat x y))))
    (put 'mul '(rational rational)
         (fn [x y] (tag (mul-rat x y))))
    (put 'div '(rational rational)
         (fn [ x y] (tag (div-rat x y))))
    (put 'make 'rational
         (fn [n d] (tag (make-rat n d))))))

(install-rational-package)

(defn make-rational [n d]
  ((get 'make 'rational) n d))


(defn install-complex-package []
  (let [make-from-real-imag (fn [x y] ((get 'make-from-real-imag 'rectangular) x y))
        make-from-mag-ang (fn [r a] ((get 'make-from-mag-ang 'polar) r a))
        add-complex (fn [ z1 z2] (make-from-real-imag (+ (real-part z1) (real-part z2))
                                                     (+ (imag-part z1) (imag-part z2))))
        sub-complex (fn [ z1 z2]
                      (make-from-real-imag (- (real-part z1) (real-part z2)) (- (imag-part z1) (imag-part z2))))
        mul-complex (fn [z1 z2] (make-from-mag-ang (* (magnitude z1) (magnitude z2))
                                                  (+ (angle z1) (angle z2))))
        div-complex (fn [z1 z2]
                      (make-from-mag-ang (/ (magnitude z1) (magnitude z2)) (- (angle z1) (angle z2))))
        tag (fn [z] (attach-tag 'complex z))]
    (put 'add '(complex complex)
         (fn [z1 z2] (tag (add-complex z1 z2))))
    (put 'sub '(complex complex)
         (fn [z1 z2] (tag (sub-complex z1 z2))))
    (put 'mul '(complex complex)
         (fn [z1 z2] (tag (mul-complex z1 z2))))
    (put 'div '(complex complex)
         (fn [z1 z2] (tag (div-complex z1 z2))))
    (put 'make-from-real-imag 'complex
         (fn [x y] (tag (make-from-real-imag x y))))
    (put 'make-from-mag-ang 'complex
         (fn [r a] (tag (make-from-mag-ang r a))))
    (put 'real-part '(complex) real-part)
    (put 'imag-part '(complex) imag-part)
    (put 'magnitude '(complex) magnitude)
    (put 'angle '(complex) angle)))

(install-complex-package)

(defn add-complex [z1 z2]
  (make-from-real-imag (+ (real-part z1) (real-part z2))
                       (+ (imag-part z1) (imag-part z2))))
(defn sub-complex [z1 z2]
  (make-from-real-imag (- (real-part z1) (real-part z2))
                       (- (imag-part z1) (imag-part z2))))
(defn mul-complex [z1 z2]
  (make-from-mag-ang (* (magnitude z1) (magnitude z2))
                     (+ (angle z1) (angle z2))))

(defn div-complex [z1 z2]
  (make-from-mag-ang (/ (magnitude z1) (magnitude z2))
                     (- (angle z1) (angle z2))))


(defn make-complex-from-real-imag [x y] ((get 'make-from-real-imag 'complex) x y))

(defn make-complex-from-mag-ang [r a] ((get 'make-from-mag-ang 'complex) r a))
