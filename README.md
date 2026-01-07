# SuperSmoothBottomBar (平滑底部导航栏)

**`SuperSmoothBottomBar`** 是一个 Android 底部导航栏库。

## DEMO效果

<img src="https://github.com/yjz0221/yjzsmoothnavbar/blob/main/demo.gif" alt="demo" style="zoom:60%;" />



## 📦 安装说明

### 第一步：添加 JitPack 仓库

在项目的根目录 `settings.gradle` 或 `build.gradle` 中添加：

```Gradle
dependencyResolutionManagement {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```



### 第二步：添加依赖

在 App 模块的 `build.gradle` 中添加：

```kotlin
dependencies {
    implementation("com.github.yjz0221:yjzsmoothnavbar:1.0.0")
}
```



## 🚀 使用指南

### 1. 定义菜单资源

在 `res/menu/` 下新建 `menu_bottom.xml`：

```xml
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item
        android:id="@+id/item_home"
        android:icon="@drawable/ic_home"
        android:title="首页"/>
    <item
        android:id="@+id/item_scan"
        android:icon="@drawable/ic_scan"
        android:title="扫描"/>
    <item
        android:id="@+id/item_profile"
        android:icon="@drawable/ic_profile"
        android:title="我的"/>
</menu>
```



### 2. 在布局中使用 (XML)

```xml
<com.github.yjz.widget.nav.SuperSmoothBottomBar
    android:id="@+id/bottomBar"
    android:layout_width="match_parent"
    android:layout_height="70dp"
    android:layout_alignParentBottom="true"
    android:layout_margin="16dp"
    
    app:ssb_menu="@menu/menu_bottom"
    app:ssb_backgroundColor="#FFFFFF"
    
    app:ssb_orientation="horizontal" 
    
    app:ssb_barCornerRadius="35dp"
    app:ssb_roundCorners="all"
    
    app:ssb_indicatorColor="#FFD600"
    app:ssb_indicatorRadius="10dp"
    app:ssb_indicatorMarginVertical="5dp"
    
    app:ssb_textSize="14sp"
    app:ssb_iconSize="24dp"
    app:ssb_itemPadding="8dp" />
```



### 3. 代码设置监听 (Kotlin)

```kotlin
val bottomBar = findViewById<SuperSmoothBottomBar>(R.id.bottomBar)

// 选中监听
bottomBar.onItemSelected = { position ->
    when (position) {
        0 -> switchFragment(HomeFragment())
        1 -> switchFragment(ScanFragment())
        2 -> switchFragment(ProfileFragment())
    }
}

// 重复点击监听 (可选，用于刷新或回到顶部)
bottomBar.onItemReselected = { position ->
    // do something...
}

// 代码控制选中
bottomBar.setActiveItem(1)
```



## 🎨 属性文档 (Attributes)

您可以通过以下 XML 属性全方位定制导航栏的外观：

| **属性名**                    | **类型**  | **描述**                                                     | **默认值** |
| ----------------------------- | --------- | ------------------------------------------------------------ | ---------- |
| **`ssb_menu`**                | reference | 引用 `res/menu` 下的 XML 文件，用于自动生成 Tab 选项         | null       |
| **`ssb_orientation`**         | enum      | **核心属性**：控制 Item 内部布局风格。 `horizontal`: 左右结构 (胶囊式)。 `vertical`: 上下结构 (传统式)。 | horizontal |
| `ssb_backgroundColor`         | color     | 导航栏的背景颜色                                             | White      |
| `ssb_barCornerRadius`         | dimension | 导航栏外轮廓的圆角半径 (配合 `ssb_roundCorners` 使用)        | 0          |
| `ssb_roundCorners`            | flag      | 控制哪几个角显示圆角 (支持组合：`topLeft|topRight`, `top`, `all` 等) | all        |
| `ssb_indicatorColor`          | color     | 选中指示器(滑块)的颜色                                       | #FFD600    |
| `ssb_indicatorRadius`         | dimension | 指示器的圆角半径                                             | 10dp       |
| `ssb_indicatorMarginVertical` | dimension | 指示器距离顶部和底部的边距 (值越大，指示器越细)              | 5dp        |
| `ssb_alwaysShowText`          | boolean   | 文字和图标始终同时显示                                       | false      |
| `ssb_textSize`                | dimension | 文字大小                                                     | 12sp       |
| `ssb_iconSize`                | dimension | 图标的宽和高                                                 | 24dp       |
| `ssb_iconTint`                | color     | **未选中**状态下的图标、文字颜色                             | Gray       |
| `ssb_iconTintActive`          | color     | **选中**状态下的图标、文字颜色                               | Black      |
| `ssb_sideMargins`             | dimension | 导航栏最左侧和最右侧的内边距                                 | 10dp       |
| `ssb_itemPadding`             | dimension | Item 内部图标和文字之间的间距                                | 10dp       |



## ⚖️ 开源协议

```
Copyright [2026] [yjz]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
