sed -i 's/isMinifyEnabled = false/isMinifyEnabled = true\n      isShrinkResources = true/g' app/build.gradle.kts
sed -i '/\/\/ implementation/d' app/build.gradle.kts
