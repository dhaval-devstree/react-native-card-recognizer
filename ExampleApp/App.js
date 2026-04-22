import { useState } from 'react';
import { Button, StyleSheet, Text, View } from 'react-native';
import { scanPaymentCard } from 'react-native-card-recognizer';

const App = () => {

  const [cardDetails, setCardDetails] = useState(null);

  const _onPressScanCardButton = async () => {
    try {
      const card = await scanPaymentCard({
        permission: { title: 'Camera Permission', description: 'ExampleApp would like to access your camera to scan a payment card.' }
      });
      setCardDetails(card)
    } catch (error) {
      console.log('error ==> ', error);
    };
  };

  return (
    <View style={styles.mainContainer}>
      <Button title='Scan Card' onPress={_onPressScanCardButton} />
      {cardDetails ?
        <View style={styles.subContainer}>
          {cardDetails?.cardNumber ? <Text>Card Number : {cardDetails?.cardNumber}</Text> : null}
          {cardDetails?.cardHolderName ? <Text>Card Holder Name : {cardDetails?.cardHolderName}</Text> : null}
          {cardDetails?.expirationDate ? <Text>Expiration Date : {cardDetails?.expirationDate}</Text> : null}
        </View> : null}
    </View>
  );
};

export default App;

const styles = StyleSheet.create({
  mainContainer: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center'
  },
  subContainer: {
    marginTop: 10,
    marginHorizontal: 10
  }
});