import 'babel-polyfill';

import App from './components/App';
import AppHomeRoute from './routes/AppHomeRoute';
import React from 'react';
import ReactDOM from 'react-dom';
import Relay from 'react-relay';
import RelayNetworkDebug from 'react-relay/lib/RelayNetworkDebug';

RelayNetworkDebug.init();

ReactDOM.render(
  <Relay.RootContainer
    Component={App}
    route={new AppHomeRoute()}
  />,
  document.getElementById('root')
);
