# plugin-cloudinary

使用 Cloudinary 提供的服务来优化 Halo 主题端的图片媒体资源以提高网站的图片资源加载速度和性能。

## How does it work?

1. 通过拦截主题端的 HTML 页面，解析其中的 `<img>` 元素得到图片资源的 URL。
2. 通过 Cloudinary 提供的 API 生成不同尺寸且格式为 Webp 的图片资源 URL。
3. 修改 HTML 页面中的 `<img>` 元素，增加 `srcset` 属性，以便浏览器根据不同的屏幕尺寸选择合适的图片资源加载。

通过以上步骤，可以有效地提高网站的图片资源加载速度和性能。

## Configuration

1. 在 [Cloudinary](https://console.cloudinary.com/) 注册账号。
2. 在 Cloudinary 控制台的 Getting Started 页面中找到 Cloud name、API Key 和 API Secret 信息，可以点击 View Credentials 查看。
3. 在 Halo 后台的插件管理中配置此插件的 Cloud name、API Key 和 API Secret 信息为对应的值。
4. 保存配置并清理一下页面缓存就已经生效了。
