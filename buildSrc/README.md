## Steps to manage dependencies with buildSrc

1.  Define dependencies in the `Dependencies.kt`.

   - Define the lib name in `Libs ` block like below:

     ``` kotlin
     val supportV4 = "androidx.legacy:legacy-support-v4:${Versions.supportV4}"
     ```

   - Define the lib version name in `Versions` block like below:

     ``` kotlin
     val supportV4 = "1.0.0"
     ```

2. When working on a module, add dependencies in the `dependencies` block:

     - Local libraries:

       ``` groovy
       implementation project(':libraries:fabscreen-legacy')
       ```

     - Remote libraries:
     
       ``` groovy
       implementation Libs.INSTANCE.supportV4
       ```
      
     - AnnotationProcessors:
      
       ```groovy
       annotationProcessor Libs.INSTANCE.butterknifeAnnotation
       ```
