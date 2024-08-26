# Known Bugs
BUG: "enter" when creating a new exercise component does weird stuff (should trim+enter)
BUG: focus is set to autocomplete on addnewexercise when a component has been added and popup closed
BUG: resume activity from icon (not notification) reopens app completely
BUG: exercise added started with achivements already set?
BUG: adding first set is swallowed when rep edit text has focus (toast shows! inputs cleared) - related to achievements? worked on latest exercise but not earlier
- BUG: I have animations turned off... lol
Bug: notification timer not restarted on new set
Bug: adding new set not updating the exercise?
Bug: total set time doesn't count when in background
Bug: achievement doesn't survive reload
Bug: app not installed when starting from bottom bar icon


# TODO
- workout creation based on body part
- multiple of same set should show better in current workout (collapse?)
- Why would history not work? - because exercise was added by manually recreating instead of selecting from list!
- show how often an exercise is done on the overall list
- move from a group selecter to a difficulty selector that includes bodyweight
- action bar on workout could use better styling (, back button, etc)
- spinners should be searchable
- ability to reorder exercises in workout
- nicer page scrolling animations between tabs on "exercises"
- I keep wanting to tap "add exercise" on empty workout
- be able to back up exercises for future migrations
- able to edit exercises (not just remove)
- when achieving a record, record when it was achieved
- when editing a component that causes the list of components of that type to be reordered, that refresh should refresh a range and not entire dataset
- Bodyweight - what kg to enter?
- add average weight per set and rep in the workout overview page
- basic weight tracking
- better icons for navigation panes
- profile page
- better text for the notification action
- add graphs for exercise history
- profile statistics
- better logo and splash screen
- fix icon (lighter background, missing shine on second stickout on F)
- - add a dumbbell in bottom right of logo
- better font
- the weight + reps enters should have more strict validation provided by android itself?
- better icons for achievements
- timer pause icon should be closer to time, better styled, and timer started/stops when pressing anywhere in layout
- number of sets at current weight in notification should be displayed better (closer to the weight?)
- records should live with their exercises forever

# Out of Scope
- tint of chips should be per bodypart - right now its ordered by most frequent
- bring points to the spinners on exercise type for more flexibility?
- most common exercises should be based on similar exercises already in workout?
- total points per exercise in chip next to exercise name (floating popups enough?)
- BUG: buttons dont line up with spinners in adding exercise (problem only if the spinner is empty) - should happen once in entire app lifecycle