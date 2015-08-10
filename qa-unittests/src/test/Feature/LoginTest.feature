# Created by cbn at 8/10/15
Feature: Login Action
  Login to CollectionSpace

  Scenario: Successful  Login with Valid creds
    Given user is on Home Page
    When User Navigates to Login Page
    And User enters UserName and Password
    Then Message displayed Login Successfully