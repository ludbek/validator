## validator

[![Clojars Project](https://img.shields.io/clojars/v/validator.svg)](https://clojars.org/validator)

## Features
- functions as validators
- strict and partial validations for web APIs
- easy custom error messages for i18n

## Walkthrough

```clojure
(require '[validator.core :refer [validate]])

; Validators are simple functions that takes a value and returns an error.
(defn is-required [value all-values field]
  (if (or (nil? value) (= value ""))
    "This field is required."))

; We can make use of closure to create sophisticated validators
(defn confirm-password [password-field]
  (fn [value all-values field]
    (if (not (= value (get all-values password-field)))
      "The passwords are not equal.")))

; If the values are invalid it returns a map of errors
(validate {:username "" :password "apassword" :confirm-password "bpassword"}
          {:username is-required :password is-required :confirm-password (confirm-password :password)})
; {:username "This field is required.", :confirm-password "The passwords are not equal."}

; If the values are valid it returns nil
(validate {:username "ausername" :password "apassword" :confirm-password "apassword"}
          {:username is-required :password is-required :confirm-password (confirm-password :password)})
; nil
```

## validate
`validate` function takes `data`, `schema` and optional `config`.
`config` is a map which defaults to `{:strict true :patch false}`

### Strict validation
By default `validate` complains if we give unknown fields.
We can disable it by passing `{:strict false}` to it as the third argument.

```clojure
(require '[validator.core :refer [validate]])

(defn is-required [value all-values field]
  (if (or (nil? value) (= value ""))
    "This field is required."))

; It complains about uknown field
(validate {:username "ausername" :address "some value"}
          {:username is-required})
; {:address "Unknown field."}

; It ignores the unkown fields
(validate {:username "ausername" :address "some value"}
          {:username is-required}
	  {:strict false})
; nil
```

### Partial validation
By default `validate` complains if any fields are absent.
We can ask it to validate partial data by passing `{:patch true}`.

This is useful for PATCH requests in the APIs.

```clojure
(require '[validator.core :refer [validate]])

(defn is-required [value all-values field]
  (if (or (nil? value) (= value ""))
    "This field is required."))

; It complains about :address
(validate {:username "ausername"}
          {:username is-required :address is-required})
; {:address "This field is required."}

; It won't complains about :address
(validate {:username "ausername"}
          {:username is-required :address is-required}
	  {:patch true})
; nil
```

## Custom validators
Validators are simply functions that takes a value and returns an error or `nil`.
A validator must take 3 arguments.

1. the value
2. the map of all the values passed to `validate`
3. and the field itself

Below is a validator called `is-required` which returns an error if a value is `nil` or
empty string `""`.

```clojure
(defn is-required [value all-values field]
  (if (or (nil? value) (= value ""))
    "This field is required."))
```

`validator` comes with few essential built in validators.

## Built in validators
### required
### equals-to
### min-len
### max-len
### email
### is-boolean
### is-number
### is-string
### enum
