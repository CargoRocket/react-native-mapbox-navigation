import PropTypes from 'prop-types';
import * as React from 'react';
import { requireNativeComponent, StyleSheet, UIManager, findNodeHandle } from 'react-native';

class MapboxNavigation extends React.Component {
  updateRoute(payload) {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(this.refs.navigationView),
      1, // UIManager.RNMapboxNavigation.Commands.updateRoute,
      [payload]
    );
  }

  render() {
    return <RNMapboxNavigation ref="navigationView" style={styles.flex} {...this.props} />;
  }
};

MapboxNavigation.propTypes = {
  origin: PropTypes.array.isRequired,
  destination: PropTypes.array.isRequired,
  shouldSimulateRoute: PropTypes.bool,
  routes: PropTypes.string,
  onLocationChange: PropTypes.func,
  onRouteProgressChange: PropTypes.func,
  onError: PropTypes.func,
  onCancelNavigation: PropTypes.func,
  onArrive: PropTypes.func,
};

const RNMapboxNavigation = requireNativeComponent(
  'MapboxNavigation',
  MapboxNavigation
);


const styles = StyleSheet.create({
  flex: {
    flex: 1,
  },
});

export default MapboxNavigation;
