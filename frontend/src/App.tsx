import { RouterProvider } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ConfigProvider, type ThemeConfig } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { router } from './router';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

const forumTheme: ThemeConfig = {
  token: {
    colorPrimary: '#18A999',
    colorInfo: '#2878D8',
    colorWarning: '#F4B942',
    colorTextBase: '#17202A',
    colorBgBase: '#F7FBFA',
    colorBorder: '#D8E8E4',
    colorBorderSecondary: '#E8F1EE',
    borderRadius: 8,
    controlHeight: 40,
    fontFamily:
      '"HarmonyOS Sans SC", "PingFang SC", "Microsoft YaHei", "Noto Sans CJK SC", sans-serif',
  },
  components: {
    Button: {
      borderRadius: 8,
      primaryShadow: '0 10px 22px rgba(24, 169, 153, 0.22)',
    },
    Card: {
      borderRadiusLG: 8,
      headerBg: 'transparent',
    },
    Input: {
      borderRadius: 8,
      activeBorderColor: '#18A999',
      hoverBorderColor: '#8EDDD3',
    },
    Layout: {
      bodyBg: '#F7FBFA',
      headerBg: 'rgba(255, 255, 255, 0.9)',
      footerBg: 'transparent',
      siderBg: '#F1F8F6',
    },
    Menu: {
      itemBorderRadius: 8,
      itemSelectedBg: '#E5F7F4',
      itemSelectedColor: '#108A7D',
      itemColor: '#5A6B75',
      itemHoverColor: '#108A7D',
    },
  },
};

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ConfigProvider locale={zhCN} theme={forumTheme}>
        <RouterProvider router={router} />
      </ConfigProvider>
    </QueryClientProvider>
  );
}

export default App;
