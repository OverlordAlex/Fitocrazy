# Known Bugs

# TODO
- oh god adding sets makes noise??
- exercises should use text entry to suggest next set
- update SDK target (edge to edge, intents, and string formatting to be checked)
- fail color should be applied to the specific line, not title
- Use quantity strings https://developer.android.com/guide/topics/resources/string-resource#Plurals
- build a workout by body parts
    - done by offering the next exercise based on past stats? (should I be copying workouts more)
- suggest sets based on past experience
- Failed weight can't be tracked
- New award for most points?
- Workouts should also shine if new records set
- set records should use proper localisation and formatting
- add record type for "best set" which explains the most-moved record
- be able to toggle sort most common exercises by date desc/asc
- Calculation should be based more heavily on weight over reps
- When going to old exercise and back, then should scroll to where it was clicked from in the list
- add exercise to current workout from search in other list
- move from a group selecter to a difficulty selector that includes bodyweight
- action bar on workout could use better styling (, back button, etc)
- spinners should be searchable
- nicer page scrolling animations between tabs on "exercises"
- when pressing a chip for the number of times that exercise was done, open a view of the exercises when it was last seen
- Bodyweight - what kg to enter?
- add average weight per set and rep in the workout overview page
- better icons for navigation panes
- better text for the notification action
- profile statistics
- better logo and splash screen
- fix icon (lighter background, missing shine on second stickout on F)
- - add a dumbbell in bottom right of logo
- better font
- better icons for achievements
- timer pause icon should be closer to time, better styled, and timer started/stops when pressing anywhere in layout
- number of sets at current weight in notification should be displayed better (closer to the weight?)


# Out of Scope
- allow selection of colours for resting/work/etc
- tint of chips should be per bodypart - right now its ordered by most frequent
- bring points to the spinners on exercise type for more flexibility?
[SOLVED!] most common exercises should be based on similar exercises already in workout?
- BUG: buttons dont line up with spinners in adding exercise (problem only if the spinner is empty) - should happen once in entire app lifecycle
- multiple of same set should show better in current workout - collapse? (not needed, is fine in practice)
[no longer triggers] BUG: "enter" when creating a new exercise component does weird stuff (should trim+enter)
[no longer triggers] BUG: adding first set is swallowed when rep edit text has focus (toast shows! inputs cleared) - related to achievements? worked on latest exercise but not earlier
[no longer triggers] BUG: I have animations turned off... lol
[no longer triggers] Bug: app not installed when starting from bottom bar icon
[no longer triggers] BUG: Search is not working