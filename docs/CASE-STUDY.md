# PIXLO: fixing accidental habit check-ins

## Problem

A home-screen widget recorded a daily habit when tapped, but an accidental check-in could not be undone. Repeated taps were ignored, leaving the user unable to correct the mistake.

## Requirement

The same widget should toggle only today's check-in. Day one alternates between 0 and 1. The second consecutive day alternates between 1 and 2. Repeated taps must not reset yesterday's progress or award extra days.

## Implementation

Before the first check-in of a day, HabitStore persists the previous count and last-check-in date for that widget. A second tap restores that state. Subsequent taps alternate between the previous state and today's completed state. Independent widget IDs keep separate snapshots. A compatibility fallback supports widgets checked in before the update.

## Validation

Four new JVM regression tests cover first-day/second-day repeated taps, restoring history after missed days and an existing widget without a snapshot. The local suite now has 12 passing tests. Lint and debug APK assembly passed. Niwas reported the updated toggle working on his phone. Two Android persistence tests are included but have not been executed locally. Overnight rollover and launcher-specific behaviour remain device-check items.

## Development

This project was developed with AI coding assistance. Requirements, iteration and user testing are documented rather than presented as independently authored code or a store release.
