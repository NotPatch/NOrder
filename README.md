# NOrder

A comprehensive marketplace plugin for Minecraft Paper servers that allows players to create buy orders for items. Players can place orders for items they need, and other players can fulfill these orders by delivering the requested items for payment.

## Features

- **Create Buy Orders**: Players can create orders for items they need, specifying quantity and price per item
- **Order Management**: View and manage your active orders through an intuitive GUI
- **Order Fulfillment**: Deliver items to active orders and earn money instantly
- **Order Highlighting**: Pay a fee to highlight your order for better visibility
- **Progress Tracking**: Visual progress bars show order completion status
- **Time-Limited Orders**: Orders automatically expire after a configurable period
- **Database Support**: Support for both SQLite and MySQL databases
- **Statistics Tracking**: Track player order statistics and delivery history
- **Discord Webhooks**: Send notifications to Discord for order events
- **Multi-Language Support**: Built-in support for multiple languages (English, Turkish)
- **PlaceholderAPI Integration**: Use order data in other plugins via placeholders
- **Vault Economy Integration**: Seamless integration with Vault-compatible economy plugins
- **Item Blacklist**: Prevent certain items from being ordered
- **Order Limits**: Set limits on active orders per player
- **Permission-Based Features**: Fine-grained control over plugin features

## Requirements

- **Server**: Paper 1.21.4 or higher
- **Java**: Java 21 or higher
- **Dependencies**:
  - Vault (Required)
  - PlaceholderAPI (Optional)
  - NLib (Required)

## Installation

1. Download the latest release of NOrder
2. Place the `.jar` file in your server's `plugins` folder
3. Install required dependencies (Vault)
4. Restart your server
5. Configure the plugin in `plugins/NOrder/config.yml`
6. Reload the plugin with `/orderadmin reload`

## Configuration

### Main Configuration (`config.yml`)

```yaml
database:
  type: sqlite                # Database type (sqlite or mysql)
  host: localhost             # MySQL host
  port: 3306                  # MySQL port
  database: norder            # Database name
  username: root              # MySQL username
  password: your_secure_password  # MySQL password

lang: en_US                   # Language (en_US or tr_TR)

settings:
  highlight-fee: 2.5          # Fee percentage for highlighting orders
  send-webhooks: false        # Enable Discord webhooks

# Order expiration time is set per player with permission
# norder.expiration.<days> (default: 7 days)

# Order limits are set per player with permission
# norder.limit.<number> (default: 5)
```

### Item Blacklist

Configure items that cannot be ordered in `config.yml`:

```yaml
blacklist-items:
  - "BEDROCK"
  - "COMMAND_BLOCK"
  - "SPAWNER"
  # Add more items...
```

## Commands

### Player Commands

| Command | Aliases | Description | Permission |
|---------|---------|-------------|------------|
| `/order` | `/norder`, `/sipariş` | Open the order menu | `norder.use` |

### Admin Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/orderadmin reload` | Reload plugin configurations | `norder.admin` |
| `/orderadmin info <id>` | View detailed order information | `norder.admin` |
| `/orderadmin delete <id>` | Delete an order | `norder.admin` |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `norder.use` | Allows using the order system | op |
| `norder.menu` | Allows opening the order menu | op |
| `norder.admin` | Allows using admin commands | op |
| `norder.highlight` | Allows highlighting orders | `norder.highlight` |
| `norder.limit.<number>` | Sets max active orders (e.g., `norder.limit.10`) | 5 |
| `norder.expiration.<days>` | Sets order expiration in days (e.g., `norder.expiration.14`) | 7 |

## Usage

### Creating an Order

1. Run `/order` to open the order menu
2. Click "Create New Order"
3. Select the item you want to order
4. Set the quantity needed
5. Set the price per item
6. (Optional) Enable highlighting for better visibility
7. Click "Confirm Order"

The total cost will be deducted from your balance when the order is created.

### Fulfilling Orders

1. Run `/order` to view all active orders
2. Click on an order to view details
3. Bring the requested items in your inventory
4. Click on the order to deliver items
5. Receive payment instantly for delivered items

### Managing Your Orders

1. Run `/order` to open the order menu
2. Click "Your Orders" to view your active orders
3. View progress and collected items
4. Take delivered items from your orders

## PlaceholderAPI Placeholders

If PlaceholderAPI is installed, you can use these placeholders:

- `%norder_orders_total%` - Number of total orders

Spesific order placeholders
- `%norder_order_<id>_<>`

- `%norder_order_<id>_material%`
- `%norder_order_<id>_amount%`
- `%norder_order_<id>_price%`
- `%norder_order_<id>_buyer%`
- `%norder_order_<id>_status%`
- `%norder_order_<id>_createDate%`
- `%norder_order_<id>_expirationDate%`

Spesific player placeholders
- `%norder_player_<player>_<>`

- `%norder_player_<player>_totalOrders%`
- `%norder_player_<player>_totalEarnings%`
- `%norder_player_<player>_totalDelivered%`
- `%norder_player_<player>_totalCollected%`

## Discord Webhooks

Configure webhooks in `webhooks.yml` to receive notifications about:
- New orders created

## Multi-Language Support

The plugin supports multiple languages. Available languages:
- English (en_US)
- Turkish (tr_TR)

To add custom language files, create a new `.yml` file in `plugins/NOrder/languages/`.

## Database Support

### SQLite (Default)
No additional configuration needed. Data is stored in `plugins/NOrder/database.db`.

### MySQL
Configure MySQL settings in `config.yml`:

```yaml
database:
  type: mysql
  host: localhost
  port: 3306
  database: norder
  username: root
  password: your_password
  pool-size: 10
  minimum-idle: 5
```

## Support

For issues, feature requests, or questions:
- GitHub: [NotPatch/NOrder](https://github.com/NotPatch/NOrder)
- Create an issue on GitHub for bug reports or feature requests

## Building from Source

```bash
git clone https://github.com/NotPatch/NOrder.git
cd NOrder
mvn clean package
```

The compiled `.jar` file will be in the `target` directory.

## License

This project is licensed under the terms specified by the repository owner.

## Credits

- Developed by NotPatch
- Uses [NLib](https://github.com/notpatch/NLib) for core functionality
- Compatible with Paper server implementations
