(ns validator.core
  (:require [clojure.set :refer [difference intersection]]))

(defn noval [val]
  (or (nil? val) (= val "")))

(defn normalize-field [field]
  (-> (name field)
      (clojure.string/replace #"-" " ")
      (clojure.string/capitalize)))

(defn normalize-validators [validators]
  (if (vector? validators)
    validators
    [validators]))

(defn validate-single [value validators all-values field]
  (loop [vs (normalize-validators validators)]
    (let [this-validator (first vs)
          rest-validators (rest vs)
          error (this-validator value all-values field)]
      (if (or (not (nil? error))
              (empty? rest-validators)
              (= error :skip))
        (if (= error :skip) nil error)
        (recur (rest vs))))))

(defn validate-against-schema [data schema]
  (reduce-kv (fn [m k v]
               (assoc m k (validate-single (get data k) v data k)))
             {}
             schema))

(defn exclude-nils [errors]
  (reduce-kv #(if %3 (assoc %1 %2 %3) %1) {} errors))

(defn mark-unknown-fields [errors unknown-fields strict]
  (if (and strict (> (count unknown-fields) 0))
    (reduce #(assoc %1 %2 "Unknown field.") errors unknown-fields)
    errors))

(defn validate
  ([data schema] (validate data schema {:strict true :patch false}))
  ([data schema {strict :strict patch :patch}]
   (let [common-fields (intersection (set (keys data)) (set (keys schema)))
         unknown-fields (difference (set (keys data)) (set (keys schema)))
         this-schema (if patch
                       (reduce #(assoc %1 %2 (get schema %2)) {} common-fields)
                       schema)
         errors (-> (validate-against-schema data this-schema)
                    exclude-nils
                    (mark-unknown-fields unknown-fields strict))]
     (if (empty? errors)
       nil
       errors))))

(defn required
  [& {:keys [required? error] :or {required? true error "This field is required."}}]
  (fn [value all-values field]
    (cond
      (and required? (noval value)) (or error "This field is required.")
      (and (not required?) (noval value)) :skip)))

(defn equals-to [& {:keys [field error] :or {error nil}}]
  (fn [value all-values this-field]
    (if (not= value (get all-values field))
      (or error (str this-field " and " field " should be equal.")))))

(defn min-len [& {:keys [length error] :or {error nil}}]
  (fn [value all-values field]
    (if (< (count value) length)
      (or error (str "This field should be at least " length " characters long.")))))

(defn max-len [& {:keys [length error] :or {error nil}}]
  (fn [value all-values field]
    (if (> (count value) length)
      (or error (str "This field should be at most " length " characters long.")))))

(defn email
  "Returns an error if given value is not an email."
  [& {:keys [error] :or {error nil}}]
  (fn [value all-values field]
    (let [pattern #"[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?" ]
      (if (not (re-matches pattern (or value "")))
        (or error "It is not a valid email.")))))

(defn is-boolean
  [& {:keys [error] :or {error "This field must be a boolean."}}]
  (fn [value all-values field]
    (if (boolean? value)
      nil
      error)))

(defn is-number
  [& {:keys [error] :or {error "This field must be a number."}}]
  (fn [value all-values field]
    (if (number? value)
      nil
      error)))

(defn is-string
  [& {:keys [error] :or {error "This field must be a string."}}]
  (fn [value all-values field]
    (if (string? value)
      nil
      error)))

(defn enum
  [& {:keys [values error] :or {error "Invalid value."}}]
  (fn [value all-values field]
    (if (contains? values value)
      nil
      error)))
