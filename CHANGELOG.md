# 17 AUG 2024
- BUG: Ordering of component frequency is wrong on workout list
- BUG: Layout shifted up on resume - could be from keyboard focus on edit text causing keyboard to show and shift (fixed layout to shift correctly)
- moved Add new exercise to its activity 


# 16 AUG 2024 (Navigation)
- Added three pane navigation
- Created CHANGELOG.md and README.md


# 15 AUG 2024 (Suspend/Resume)
- highlight good points totals
- BUG: points dont make sense when adding and removing - could be related to bonus? in fact all stats are not loaded correctly
- BUG records are not updated when set is removed?~~ <- its a view? shouldnt be possible
- the workout overview should format date as "today" when appropriate
- toast/floating up emoji on adding points
- handle background running and all on-resume stuff
- BUG: null pointer on notification trying to set exercise? (presumed solved)
- BUG adding sets when activity is in background is broken (notification received twice?)
- BUG adding sets doesnt refresh view (should detect if foreground?)
- changes for suspend/resume already identified
- notification action doesnt work if app backgrounded for a long time (either no workout added, or duplicate empty workouts created)
- dont show toast if activity is in the background
- fix points on loading old workouts (points are read from DB instead of calculating


# 14 AUG 2024 (Points)
- points should be wholistic across all sets in an exercise
- historical sets should be eager-loaded when exercise added to workout (and not in bind viewholder)
- number of sets in notifcation should be the number at the current weight
- move calculation of points from companion object to on workout itself as general update
- go back to adding from the top - the new exercise button becomes confusing otherwise
- BUG: when too many body-part chips are assigned to an exercise it causes the remove-set button to hide
- - Fixed by limiting to only 3 chips
- exercise PRs in the exercise card in workout?


# 12 AUG 2024 (Notification)
- make the "add exercises" text visible again
- don't round corners on list views
- update when exercise added, set added, set removed
- only show when workout is today!
- only show on workout activity
- pendingIntent on button press to trigger add set
- pendingIntent to reopen app
- only dismiss notification on workout saved, or back button (not home)
- enhance set data with the number of sets at this weight


# 11 AUG 2024
- refactor ExerciseList to use ExerciseView instead of Pair
- notification panel


# 10 AUG 2024 (Bug hunt)
- BUG: total_time timer resets when adding a new set on today (for saved workouts)
- timer can be paused and restarted
- BUG: adding a new workout puts it at the bottom instead of the top
- BUG: sets added to previous workouts show up as "today"
- style popups for adding exercises and components


# 09 AUG 2024 (Theming)
- color and theme main page
- color and theme workout page
- delete workouts on long-press
- close delete-workout button on scroll
- db.Exercise should link to a workoutID and not a date to group exercises
- - and delete should delete associated exerciseId and Set
- BUG: delete button shows twice sometimes


# 08 AUG 2024
- refresh DB workout list on result


# 07 AUG 2024 (Start of recording changes)
- workout list on main page
- workout selection on main page
- resumable timers
- ensure timers don't start if adding sets to historical exercises
- verify that historical exercises show their "today" sets appropriately
- style main page strings
-  chip group for body parts per exercise
- total chip group per exercise