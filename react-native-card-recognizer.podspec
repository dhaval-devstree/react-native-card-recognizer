Pod::Spec.new do |s|
  s.name         = 'react-native-card-recognizer'
  s.version      = '0.0.1'
  s.summary      = 'iOS payment card scanner native module for React Native'
  s.homepage     = 'https://github.com/dhaval-devstree/react-native-card-recognizer'
  s.license      = { :type => 'UNLICENSED' }
  s.author       = { 'dhaval-devstree' => 'dev@buddy.invalid' }
  s.platform     = :ios, '15.1'
  s.source       = { :path => '.' }

  s.source_files = 'ios/**/*.{h,m,mm,swift}'
  s.swift_version = '5.0'

  s.dependency 'React-Core'
  s.frameworks = 'AVFoundation', 'Vision', 'UIKit', 'CoreMedia', 'CoreVideo'
end