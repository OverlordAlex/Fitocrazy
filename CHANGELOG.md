# 11 Dec 2024
BUG: Screen recording of bodypart chips behaving weird on adding exercise - Bodypart chips in workout act weird - top exercise gets chips of second if second has a set added
Further guarding against long names


# 10 Dec 2024
- BUG: New exercise can still get awards if going up in weight within the same set group
- BUG: Exercise names on cards don't actually wrap correctly (if word is too long half is cut off but next word starts on new line)
  - solved with Marquee, but may be revisited if not working without animations 
- Workout Bodypart summary chips should be based on sets done, not just exercises


# 07 Dec 2024 (Improved graphs)
- BUG: Historical sets in workout are in reverse order
- BUG: Max graph taking the last set in the exercise instead of max
- Improved graphs - should accurately draw max weight and max moved
- BUG: Last performed date in adding new exercises is wrong (shows when exercise first added, not last done)
- Adding new exercise should reset and pause timer


# 14 Sep 2024 (Graph exercise records) v4.0 (db v16)
- Total sets in exercise so far would be nice
- bodyparts are now ordered during the workout
- up next, charts!
- added graphs to exercise history
- fixed adding records


# 12 Sep 2024 (Workout list filter by year + month)
- Fixed total workout timer
- manage time, and saving time
- Bug: total set time doesn't count when in background
- Added ability to select year + month to filter workout list
Presumed fixed:
- Scroll to top when adding new exercise <- because no animations?
- Changing date on workout should set total time to 0


# 04 Sep 2024 (Continue refactor)
- refactored suggestion list to use displayAdapter
- fixed flicker on removing exercises in workout
- Searches should split input on space and match-all
- Add exercise spinner should select added element
- Trim whitespace on search
- When adding an exercise, search is still clickable when building exercise
- remove records when removing sets
- Make "done" button on adding exercises floating so that it's always on screen and easy to hit


# 03 Sep 2024 (Continue refactor)
- added notifications
- moved achievements to the Exercise object
- Bugs assumed fixed:
  - Bug: achievement doesn't survive reload
  - Bug: adding new set not updating the exercise?
  - Bug: notification timer not restarted on new set
  - BUG: exercise added started with achivements already set?
  - BUG: resume activity from icon (not notification) reopens app completely
  - BUG Why would history not work? - because exercise was added by manually recreating instead of selecting from list?
  - be able to back up exercises for future migrations
  - when achieving a record, record when it was achieved
  - when editing a component that causes the list of components of that type to be reordered, that refresh should refresh a range and not entire dataset
  - the weight + reps enters should have more strict validation provided by android itself?
  - records should live with their exercises forever
  - total points per exercise in chip next to exercise name (floating popups enough?)
- Feedback assumed addressed:
  - Achievements and history not shown (lost on rebind of view holder?)
    - rebinding with reorder does not cause history to show
    - reopening workout does not cause it to show
      - new achievements are aware of history, but points and view look like new exercise
  - If there is one exercise it should not be reorderable
  - Total points and exercise points are different??
  - Notification timer reset on resume (problem if set started from notification action)
  -  Set timer not reset when adding from notification
  -  Items that can be gone, should be gone by default and shown only if necessary
  - Editing workouts date should default date selector to workout date and not today's date    
  - History not working could be related to the date of exercises added on same day but the date edit happens before or after they're added to the workout?
  - BUG: focus is set to autocomplete on addnewexercise when a component has been added and popup closed


# 02 Sep 2024 (Massive refactor)
- refactor component list to use adapter
- refactor exercise list to use adapter
- refactor workout list to use adapter
- refactor exercises in workout to to use adapter
- fix previous set history!


# 27 Aug 2024 (Workout Duplication) v3.0 (db v14)
- icons for moving exercises up and down
- change edit icon when in moving mode
- add button to duplicate historical workout


# 26 Aug 2024 (Activity lifecycle and reordering exercises)
- minor styling 
- Recent exercises should be in reverse date order
- Color the points chip orange when the exercise is being done for the first time
- Double checked that no more scrolling is happening on any recycler
- change notification register to only happen onCreate and onDestroy
- refresh workout onResume
- notification should work better now 
- Starting timer manually doesn't update notification?
- Recycler not refreshing state on app resume
- BUG: adding sets from notification could happen multiple times
- Removing set should toast
- Renamed layouts to consistent naming style
- show how often an exercise is done on the overall list
- Tapping "add exercise" on empty workout will open add-new-exercise dialog
- ability to reorder exercises in workout


# 22 Aug 2024 (Edit Workout dates)
- Enabled schema migrations
- - auto migrate DB / reset DB
- add bodypart chips to exercises in add-exercise, and then be able to search by them
- exercise suggestion box is now constant height and does not change one search
- edit date on long press title
- BUG: can delete exercises in use, causing crashes on those workouts


# 21 Aug 2024 (Small QoL Improvements)
- Fix application name lmao
- BUG: recyclerview does not shift up on softkeyboard open
- BUG: spinners should be sorted (searchable?)
- BUG: touch target to add new exercise too small
- BUG: searchviews too small - only grab focus when pressing on the icon
- BUG: top 5 exercises loaded for suggestions, but search bar implies could be more...
- tapping bar in workout should go home
- bodypart chips should be ordered alphabetically
- BUG: records should reset if set removed
- cannot get achievements first time ever doing set
- - dont show achievements on first instance of that exercise ever (no history)
- - should achievements be per-row? or at least on the set card itself?
- BUG: achievement icons overlapp with chips
- BUG: points toast shows weird values
- icon to indicate time can be paused


# 20 Aug 2024 (Editing Exercises) v2.0 (db v13)
- Added body part chips to exercises
- Formatted all labels
- can delete exercises that have never been used
- Added ability to add new exercise from exercise list screen
- Added exercise list sorting
- BUG: today-today-today on workout (appending instead of setting)
- if adding an exercise to a workout, if there are no possible exercises then we're in edit mode
- be able to edit date on workout


# 19 Aug 2024 (Editing Components)
- Add ability to delete components
- Add ability to rename components, including exercises
- BUG: app crashes on resuming tabbed fragment
- workout date now says "today" on the workout itself
- BUG: add component, delete immediately, navigate away and navigate back shows element still there
- need to be able to delete components (wants to put "seated" first on "seated dumbbell arnold press"
- exercise + component editor
- allow editing of exercise components
- added list of exercises


# 18 Aug 2024 (Adding Exercise Overhaul)
- Show suggested exercises at top of add new exercise
- Completion drop down should instead drop "up"
- Added a list of exercise suggestions based on frequency
  - Touch target on exercise list too small
- Exclude exercises already in current workout
- Show when the exercise was last done
- style selected exercise element
- add all selected exercises to the current workout
- include exercises never done before
- Clear edit text focus on new set added
- BUG: adding a new set when teh timer is paused does not set the color correctly
- Added component lists


# 17 AUG 2024
- BUG: Ordering of component frequency is wrong on workout list
- BUG: Layout shifted up on resume - could be from keyboard focus on edit text causing keyboard to show and shift (fixed layout to shift correctly)
- moved Add new exercise to its activity 
- BUG: workouts being created like crazy due to bad launch options (fixed with singles in manifest)
- BUG: can add exercise without tags
  - highlight when something is missing when trying to create a new exercise
- Touch target on components too small


# 16 AUG 2024 (Navigation)
- Added three pane navigation
  - full navigation over haul
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