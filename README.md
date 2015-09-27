# Android AlbumView
An android widget to display huge number of images from file system.

All images are loaded in the background thread as needed. At most only 2 bitmaps are held in memory and recycled when not showing.

## Demo
![](/screencast.gif)

## Usage

```xml
<FrameLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <com.rajasharan.widget.AlbumView
        android:id="@+id/album"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        />

</FrameLayout>
```

#### Add image paths via code

```java
String img1 = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/test1.jpg";
String img2 = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/Screenshots/test2.png";
String cameraDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/Camera";

AlbumView albumView = (AlbumView) findViewById(R.id.album);
albumView.addImage(img1)
        .addImage(img2)
        .addImages(cameraDir)
        .show();
```

## [License](/LICENSE)
    The MIT License (MIT)
