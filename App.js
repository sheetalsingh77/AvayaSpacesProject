import React from "react";
import {View, Text, Button, NativeModules} from 'react-native';

const {CounterModule, CounterKotModule, ImportandInitSDK, KotlinVideoStream} = NativeModules;


// console.log('countermodules' +JSON.stringify(ImportandInitSDK))

const App = () => {
  const hdpress = () => {
    CounterKotModule.createCounterKotPromise()
    .then((res) => console.log('hello '+ res))
    .catch((err) => console.log('err'));

    ImportandInitSDK.setupSDKInitialization((res) => { 
      console.log("init sdk "+res)
    })


  }

  const playstream = () => {
    CounterKotModule.createCounterKotPromise()
    .then((res) => console.log('hello '+ res))
    .catch((err) => console.log('err'));
    KotlinVideoStream.playVideoStream("hello1", "hello2.mp4");
  }


  return <View>
    <Text>hello</Text>
    <Button title="hello" onPress={playstream}/>
  </View>
}

export default App;