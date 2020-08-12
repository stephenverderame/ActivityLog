# ActivityLog
Android port (and upgrade) of original strava 3d project. Displays data, visualizes rides in 3D, graphs trends, shows recent activities and more
## Usage
The app allows users to sync rides from Strava, and view them in 3D, visualize trends over time, and log ride data
The app can be used to get statistics and information about a user's rides in more detail than other ride logging applications current out there
You can customize statistics to view, customize different graphs using any statistic made available in the app. You can view linear regressions to show trends, change the graph type and much more!
For example: you can graph ride time vs ride distance over a monthly interval to determine if you're endurance is increasing, decreasing or staying the same over the past months

#### Other Examples:

![Graph](https://lh3.googleusercontent.com/vdfGtsHy9YgA9EhRePzSg00IDKDC7yPlmtsJe4T7cxGKo6daqKRnYnYTuQ4V_KD6gXMO=w1536-h722-rw)
![Graph2](https://lh3.googleusercontent.com/qyxjUKw2Tl9Dj3U3z8RAEuF1Mp6z04DSiYDuZuOp-2bn3niR1AUMdJs2gyKdCOmnsTk=w1536-h722-rw)

For more information, check out the app's [google play page](https://play.google.com/store/apps/details?id=com.sev.activitylog)
## Design

![UML Diagram]( https://drive.google.com/uc?export=view&id=174SplhrscCag9rrtIZtdKXb3SrmVjg8p)

In general, the app follows an MVC design. The Activity acts as Mediator/Controller, the Views are the Views and Fragments and the Models are Models
Utilizes HTTP(S) APIs from HERE (road maps), opentopo (elevation) and Strava (Strava information)
OpenGL is used for the 3D view of rides
