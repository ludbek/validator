(ns validator.core-test
  (:require [validator.core :as v]
            [clojure.test :refer :all]))

(deftest test-normalize-field
  (testing "it works"
    (is (= (v/normalize-field :confirm-password) "Confirm password"))))

(deftest test-validate
  (testing "works with single validators"
    (let [data {}
        schema {:name (v/required) :email (v/email)}
        errors (v/validate data schema)
        expected {:name "This field is required."
                  :email "It is not a valid email."}]
      (is (= expected errors))))

  (testing "works with multiple validators"
    (let [data {:email "invalidemail"}
          schema {:email [(v/required) (v/email)]}
          errors (v/validate data schema)
          expected {:email "It is not a valid email."}]
      (is (= expected errors))))
  
  (testing "handles strict validation"
    (let [data {:full-name "full name"
                :email "an email"}
          schema {:full-name (v/required)}
          errors (v/validate data schema)
          expected {:email "Unknown field."}]
      (is (= expected errors))))

  (testing "handles partial validation"
    (let [data {:full-name "full name"}
          schema {:full-name (v/required)
                  :email (v/required)}
          errors (v/validate data schema {:patch true})]
      (is (= nil errors)))))

(deftest test-min-len
  (testing "returns an error if a value is lesser than given length"
    (let [error ((v/min-len :length 4) "123" nil nil)]
      (is (= error "This field should be at least 4 characters long."))))
  
  (testing "returns nil if a value is => than specified length"
    (let [error ((v/min-len :length 4) "1234" nil nil)]
      (is (= error nil)))))


(deftest test-required
  (testing "returns an error if the value is nil like"
    (is (= "This field is required." ((v/required) nil nil nil)))
    (is (= "This field is required." ((v/required) "" nil nil))))

  (testing "returns nil if value is not nil like"
    (is (= nil ((v/required) "a value" nil nil))))

  (testing "returns custom error message"
    (is (= "Required field." ((v/required :error "Required field.") nil nil nil))))

  (testing "returns :skip keyword if marked not required"
    (is (= :skip ((v/required :required? false) nil nil nil)))))

(deftest test-email
  (testing "returns error if the value is not email"
    (is (= "It is not a valid email." ((v/email) "anemail" nil nil))))

  (testing "returns nil if the value is email"
    (is (= nil ((v/email) "email@example.com" nil nil))))
  
  (testing "returns custom error"
    (is (= "Invalid email."
           ((v/email :error "Invalid email.") "invalidemail" nil nil)))))


(deftest test-max-len
  (testing "it returns an error"
    (is (= "This field should be at most 3 characters long."
           ((v/max-len :length 3) "abcde" nil nil))))

  (testing "it returns nil"
    (is (= nil ((v/max-len :length 3) "abc" nil nil))))

  (testing "it returns custom error"
    (is (= "Too long."
           ((v/max-len :length 3 :error "Too long.") "abcde" nil nil)))))

(deftest test-equals-to
  (testing "it returns error"
    (is (= ":confirm-password and :password should be equal."
           ((v/equals-to :field :password)
            "apassword"
            {:password "bpassword"}
            :confirm-password))))

  (testing "it returns nil"
    (is (= nil
           ((v/equals-to :field :password)
            "apassword"
            {:password "apassword"}
            :confirm-password))))

  (testing "it returns custom error"
    (is (= "Passwords are not same."
           ((v/equals-to :field :password
                         :error "Passwords are not same.")
            "apassword"
            {:password "bpassword"}
            :confirm-password)))))

(deftest test-is-boolean
  (testing "it returns error"
    (is (= "This field must be a boolean."
           ((v/is-boolean) 1 nil nil))))

  (testing "it returns nil"
    (is (= nil
           ((v/is-boolean) true nil nil))))

  (testing "it returns custom error"
    (is (= "Its not a boolean."
           ((v/is-boolean :error "Its not a boolean.") 1 nil nil)))))

(deftest test-is-number
  (testing "it returns error"
    (is (= "This field must be a number."
           ((v/is-number) "" nil nil))))

  (testing "it returns nil"
    (is (= nil ((v/is-number) 1 nil nil))))

  (testing "it returns custom error"
    (is (= "Its not a number."
           ((v/is-number :error "Its not a number.") "" nil nil)))))

(deftest test-is-string
  (testing "it returns error"
    (is (= "This field must be a string."
           ((v/is-string) 1 nil nil))))

  (testing "it returns nil"
    (is (= nil ((v/is-string) "" nil nil))))

  (testing "it returns custom error"
    (is (= "Its not a string."
           ((v/is-string :error "Its not a string.") 1 nil nil)))))

(deftest test-enum
  (testing "it returns error"
    (is (= "Invalid value." ((v/enum :values #{1 2 3 4}) 0 nil nil))))

  (testing "it retuns nil"
    (is (= nil ((v/enum :values #{1 2 3 4}) 2 nil nil))))

  (testing "it returns custom error"
    (is (= "Value not allowed."
           ((v/enum :values #{1 2 3 4} :error "Value not allowed.") 0 nil nil)))))
