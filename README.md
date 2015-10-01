Handy Dominant Color Library for extracting the dominant color from images.

Authored by Niko Schmuck (https://github.com/nikos)

Inspired by an blog post of Jared Allen (http://chironexsoftware.com/blog/?p=60)

This repo is set up with google pages so maven artifacts can be included in other projects.  See: http://blog.kaltepoth.de/posts/2010/09/06/github-maven-repositories.html

To from the command line try:

 ```
$ mvn package
$ mvn dependency:copy-dependencies
$ java -cp target/dominant-color-1.0.jar:target/dependency/* de.nava.color.BitmapDominantColor
 ```

You can then add the dependency in eclipse using ivy with the following two additions:

add this line to your ivy-settings.xml
```
<resolvers>
        <chain name="chain">
            ...
            <ibiblio name="de.nava" m2compatible="true" root="http://YOUR_GITHUB_NAME.github.io/dominant-color/repository" />
        </chain>
</resolvers>
```

and this line to your ivy.xml

```
<dependency org="de.nava" name="dominant-color" rev="1.0" />
```

To Do:

 1.  refactor to accept a BufferedImage
 1.  try using the pixel grabber so we don't need to do any byte shifting
 1.  figure out how to use the unit tests :)

