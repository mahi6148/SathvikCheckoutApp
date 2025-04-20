# Problem :
My friend [sathvik](https://github.com/sathvikkuna) and his team have to show their hardware identification reciept of their work pc, everytime they checkin and checkout from the office; they have to either show the reciept physically or a photocopy of it digitally, but searching for that one photo in the gallery everytime during check-in &out is a boring idea.

# solution:
So i came up with an idea of developing an android app which can install a widget on his home screen, the widget will show a list of photos( what ever he picks, might be family, friends, moments etc.. ) one after the other and when he clicks on the widget surface, and when he clicks on the widget it will automatically launch the app and show the homescreen where he ca show his reciept, with pinch to zoom capabilities.

## Frameworks used:
Android Jetpack:
- Glance (to build the widget UI)
- Compose (to build the app UI)
- WorkerManager (to schedule widget picture update periodically in the background)
- DataStore (to store the user selected photo Uri's and user preferences)
- Navigation(type-safe)

Kotlin:
- Coroutines (to run jobs in a seperate thread which is independent of the Main Thread)
- Serialization(to serialize and deserialize kotlin classes to be used as type safe arguements in Jetpack navigation)

Dependencies:
- Coil3 (for image management in android)
    
