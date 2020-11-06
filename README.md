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

### Multiple validators

### Short circuit validation

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

## Built in validators
`validator` comes with few essential built in validators.

### required
It returns an error if a value is either `nil` or empty string `""`.
It takes an optional argument `:error` for overriding default error.

```clojure
(require '[validator.core :refer [validate required]])

(validate {:address ""} {:address (required)})
; {:address "This field is required."}
```

#### Skip reset of the validation if the value is not required
Sometime we want to skip the validation for fields which do not have any value.
We can do that by passing `:required? false` to `required` validator.

```clojure
(require '[validator.core :refer [validate required is-string]])

(validate {:address ""} {:address [(required :required? false) (is-string)]})
; nil
```

#### Override default error message
We can override default error message.

```clojure
(require '[validator.core :refer [validate is-string]])

(validate {:address 1} {:address (is-string :error "Not a string.")})
; {:address "Not a string."}
```

### equals-to
This validator checks if a given value is equal to the value of another field
in the data. Its useful for checking if a password matches its confirmation.

```clojure
(require '[validator.core :refer [validate required equals-to]])

(validate {:password "apassword" :confirm-password "bpassword"}
          {:password (required) :confirm-password (equals-to :field :password)})
; {:confirm-password ":confirm-password and :password should be equal."}
```

#### Override default error message
We can override default error message by passing `:error` keyword argument to `equals-to`

(require '[validator.core :refer [validate required equals-to]])

(validate {:password "apassword" :confirm-password "bpassword"}
          {:password (required) :confirm-password (equals-to :field :password
	                                                     :error "Passwords do not match")})
; {:confirm-password "Passwords do not match."}

### min-len

### max-len

### email
It checks if a given value is a proper email or not.

```clojure
(require '[validator.core :refer [validate email]])

(validate {:email "invalidemail"} {:email (email)})
; {:email "It is not a valid email."}

```

We can override the default error message by passing `:error` keyword argument

```clojure
(require '[validator.core :refer [validate email]])

(validate {:email "invalidemail"} {:email (email :error "Invalid email.")})
; {:email "Invalid email."}

```

### is-boolean
It complains if the given value is not boolean.

```clojure
(require '[validator.core :refer [validate is-boolean]])

(validate {:active 1} {:active (is-boolean)})
; {:active "This field must be a boolean."}

```

We can override the default error message by passing `:error` keyword argument

```clojure
(require '[validator.core :refer [validate is-boolean]])

(validate {:active 1} {:active (is-boolean :error "Please pass a boolean value.")})
; {:active "Please pass a boolean value."}
```

### is-number
It complains if the given value is not a number.

```clojure
(require '[validator.core :refer [validate is-number]])

(validate {:distance "1km"} {:distance (is-number)})
; {:distance "This field must be a number."}
```

We can override the default error message by passing `:error` keyword argument

```clojure
(require '[validator.core :refer [validate is-number]])

(validate {:distance "1km"} {:distance (is-number :error "Please pass a number.")})
; {:distance "Please pass a number."}
```

### is-string
It complains if the given value is not a string.

```clojure
(require '[validator.core :refer [validate is-string]])

(validate {:distance 1} {:distance (is-string)})
; {:distance "This field must be a string."}
```

We can override the default error message by passing `:error` keyword argument

```clojure
(require '[validator.core :refer [validate is-string]])

(validate {:distance 1} {:distance (is-string :error "Please pass a string.")})
; {:distance "Please pass a string."}
```

### enum
It complains if a given value is not one of the enums.
It requires a keyword argument `:values` which must be a `set`.
We can also override the default error message by passing keyword argument `:error`.

```clojure
(require '[validator.core :refer [validate enum]])

(validate {:address "Madagascar"} {:address (enum :values #{"Kathmandu" "Sydney"})})
; {:address "Invalid value."}
```
