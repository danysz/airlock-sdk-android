# Airlock SDK for Android
The Airlock SDK determines which features are enabled for a particular application based on  
the user profile and device context (locale, location, device type, etc.).

## System Requirements
* Android SDK version 16 or later

## Configuring your application to use the Airlock SDK
Do the following steps to integrate the Airlock SDK with your Android project:

1. Open the `build.gradle` file of your project and add the following line to the `dependencies` code section:							
	
	`compile "com.weather.android.airlock:airlock-sdk-android:<version>"`

	Here is a sample code snippet of `build.gradle` file:
	````
	apply plugin: 'com.android.application'

	repositories {
            ....
            mavenCentral()
            ....
    }
	android {
	   
	    defaultConfig {
	    .....
	    }
	}
	
	dependencies {
	.....
	  //airlock
	  compile "com.weather.android.airlock:airlock-sdk-android:<version>"
	}
	````

1. Add the Airlock defaults file to your project.
   The defaults file contains the list of features that the SDK uses in the case information on those features is not available on the server.
   The defaults file, as a JSON file, can be downloaded via Airlock Console UI
   from the product administration section.

## SDK Overview
![](SDK.jpg)

The master list of features and configuration rules are stored on the Airlock server (AWS S3).  
Each feature definition includes rules that determine when the feature is enabled based the user profile,  
device context (locale, location, device type, etc.), and weather conditions.  
An application that uses the Airlock SDK provides the user profile, device context information,  
and weather data to the SDK. The SDK runs rules on that information and determines which features are on or off,  
and which output configurations are applied.  
In case of error or when feature and configuration information is not available on the server,  
the defaults file is used.

## Usage

To use the Airlock SDK, follow these steps:

1. Initialize the SDK by calling the **initSDK** method. This method loads the defaults file and any previously stored information from the server. You supply the product version and defaults file to this method.

2. Pull the list of features, their status, and configurations from the server by calling the **pullFeatures** method. The **pullFeatures** method downloads the current list of features from the server.

3. Calculate the pull results by calling the **calculateFeatures** method. You supply the following information to this method:

	* The device context (a single JSON string that includes the locale, location, device type, etc.)
	
	The **calculateFeatures** method determines whether the features are enabled based on the method’s input, and evaluates the feature’s configuration rules. No feature status changes are exposed until the **syncFeatures** method is called.

3. Make the current feature list available to the code by calling the **syncFeatures** method.
 
4. Use the **getFeature(feature_name).isOn** method to determine whether a feature is on or off.
	
	A condition statement surrounds every code block that is air-locked. The condition causes the block to run if the feature is enabled:

	````
	if (AirlockManager.getInstance().getFeature("feature_name").isOn()) {
	    printMap();
	}
	````
	
	**Tip**: Use the Airlock Code Assistant plugin for Android Studio to easily associate code snippets with specific features. For more information, see the [Airlock Code Assistant Readme](https://github.com/TheWeatherCompany/airlock-documentation/blob/master/README-for-airlock-ide-android/README.md).

5. Use the **getFeature(feature_name).getConfiguration** method to get the feature's configuration in JSON format.

6. Optional: Get the feature's children, parent, or all features under the root as follows:
	
	````
	AirlockManager.getInstance().getFeature("feature_name").getChildren()
	AirlockManager.getInstance().getFeature("feature_name").getParent()
	AirlockManager.getInstance().getRootFeatures()
	````

## Setting device user groups
In development only, you specify user groups that are associated with features. Each feature is associated with one or more user groups. The SDK provides a user interface that allows you to associate your device with these user groups.

![](UserGroups.jpg)

To integrate this interface into your app, do the following: 

1. Add the following lines to the `AndroidManifest.xml` file:
	````
	<application
	    ...
	    <activity
	        android:name="com.weather.airlock.sdk.ui.GroupsManagerActivity"
	        android:label="@string/user_groups_activity_title">
	        ....  
	    </activity>
	</application>
	````

2. In the app, navigate to `GroupsManagerActivity` as follows:
	````
	...
	startActivity(new Intent(this, GroupsManagerActivity.class));
	...
	````
	
##Example
   
The following sample code calls the methods that are described in the **Usage** section:
````
/* Reference to the default feature values */
int defaultFileId = R.raw.airlock_defaults;

final JSONObject profile = ... // current user profile in JSON format
final JSONObject deviceContext = ...// device context (name-value pairs structure in JSON format)
boolean updateFromServer = true;
String productVersion = “1.0.0”; // the product version number

/*
 * Initialize AirlockManager with the current context, default feature values, and product version
 */

try {
AirlockManager.getInstance().initSDK(getApplicationContext(), defaultFileId, productVersion);
} catch (AirlockInvalidFileException e) {
    e.printStackTrace();
} catch (IOException e) {
    e.printStackTrace();
}

 /*
  * Asynchronously calculates the features on-the-fly each time this function is called.
  * updateFromServer=true – Brings the new features JSON from AWS S3 (the default is true)
  * Updates LastPullTime and LastCalculateTime
  */
try {
    AirlockManager.getInstance().pullFeatures(new AirlockCallback() {
        @Override
        public void onFailure(@NonNull Exception e) {
            //on failure logic 
        }

        @Override
        public void onSuccess(String msg) {
            try {
                /**
                 * calculate the latest pullFeatures results with the current feature set
                 */
                AirlockManager.getInstance().calculateFeatures(profile, deviceContext);
                /**
                 * Synchronize the latest calculateFeatures results with the current feature set
                 * Updates LastSyncTime
                 */
                AirlockManager.getInstance().syncFeatures();
            } catch (AirlockNotInitializedException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            //on Success logic ...
        }
    });
} catch (AirlockNotInitializedException e) {
    e.printStackTrace();
}
````
## Build
Do the following steps to build the project locally:
The lasted code is tested using Android Studio 4.2.1 version
The project consists if three submodules

- airlock-sdk-common
- sdk
- sdkSample

1. Clone the project (note, the project is has submodule dependency)

```
   git clone https://github.com/IBM/airlock-sdk-android.git
   cd airlock-sdk-android
   git submodule init
   git submodule update 
```

2. To build the SDK ARR use
```
   ./gradlew sdk:build
```


## API documentation
Click here for [Airlock API Javadoc documentation](http://androidbuildserver:8080/job/airlock%20sdk/javadoc/).
