Feature: Some Feature

  Scenario: Test message
    Given I send a message on input queue
     | test          |
    Then I receive a message on output queue
     | message       |
     | test          | 
