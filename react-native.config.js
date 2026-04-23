module.exports = {
  dependency: {
    platforms: {
      android: {
        sourceDir: './android',
        packageImportPath: 'import com.reactnativecardrecognizer.CardScannerPackage;',
        packageInstance: 'new CardScannerPackage()'
      }
    }
  }
};
