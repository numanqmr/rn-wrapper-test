
# react-native-landmarksid-sdk

## Getting started

`$ npm install react-native-landmarksid-sdk --save`

### Mostly automatic installation

`$ react-native link react-native-landmarksid-sdk`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-landmarksid-sdk` and add `RNLandmarksidSdk.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNLandmarksidSdk.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.landmarksid.rn.RNLandmarksidSdkPackage;` to the imports at the top of the file
  - Add `new RNLandmarksidSdkPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-landmarksid-sdk'
  	project(':react-native-landmarksid-sdk').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-landmarksid-sdk/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-landmarksid-sdk')
  	```


## Usage
```javascript
import RNLandmarksidSdk from 'react-native-landmarksid-sdk';

// TODO: What to do with the module?
RNLandmarksidSdk;
```
  