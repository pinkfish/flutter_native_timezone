#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html
#
Pod::Spec.new do |s|
  s.name             = 'flutter_native_timezone'
  s.version          = '0.0.1'
  s.summary          = 'A native timezone project.'
  s.description      = <<-DESC
Get the native timezone from ios.
                       DESC
  s.homepage         = 'https://github.com/pinkfish/flutter_native_timezone'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'David Bennett' => 'pinkfishfrog@gmail.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.public_header_files = 'Classes/**/*.h'
  s.dependency 'Flutter'

  s.ios.deployment_target = '8.0'
end

